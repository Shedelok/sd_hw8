import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
internal class EventsStatisticImplTest {
    @Mock
    private lateinit var mockedClock: Clock

    private lateinit var eventsStatisticImpl: EventsStatisticImpl

    @BeforeEach
    fun setUp() {
        eventsStatisticImpl = EventsStatisticImpl(mockedClock)
    }

    private fun setMockedClockInstantSeconds(seconds: Long) =
        `when`(mockedClock.instant()).thenReturn(Instant.ofEpochSecond(seconds))

    @Test
    fun singleEventInLastHour() {
        val eventName = "eventName"
        val neverHappenedEventName = "neverHappenedEventName"

        setMockedClockInstantSeconds(1)
        eventsStatisticImpl.incEvent(eventName)

        setMockedClockInstantSeconds(Duration.ofHours(1).seconds)

        assertEquals(1.0 / 60, eventsStatisticImpl.getEventStatisticByName(eventName))
        assertEquals(0.0, eventsStatisticImpl.getEventStatisticByName(neverHappenedEventName))
        assertEquals(listOf(eventName to 1.0 / 60), eventsStatisticImpl.getAllEventStatistic())
    }

    @Test
    fun singleOldEvent() {
        val eventName = "eventName"
        val neverHappenedEventName = "neverHappenedEventName"

        setMockedClockInstantSeconds(1)

        eventsStatisticImpl.incEvent(eventName)

        setMockedClockInstantSeconds(1 + Duration.ofHours(1).seconds)

        assertEquals(0.0, eventsStatisticImpl.getEventStatisticByName(eventName))
        assertEquals(0.0, eventsStatisticImpl.getEventStatisticByName(neverHappenedEventName))
        assertEquals(listOf(eventName to 0.0), eventsStatisticImpl.getAllEventStatistic())
    }

    @Test
    fun oneOldAndOneNewEvent() {
        val oldEventName = "oldEventName"
        val newEventName = "newEventName"

        setMockedClockInstantSeconds(1)

        eventsStatisticImpl.incEvent(oldEventName)

        setMockedClockInstantSeconds(10)
        eventsStatisticImpl.incEvent(newEventName)

        setMockedClockInstantSeconds(1 + Duration.ofHours(1).seconds)

        assertEquals(0.0, eventsStatisticImpl.getEventStatisticByName(oldEventName))
        assertEquals(1.0 / 60, eventsStatisticImpl.getEventStatisticByName(newEventName))
        assertEquals(listOf(oldEventName to 0.0, newEventName to 1.0 / 60), eventsStatisticImpl.getAllEventStatistic())
    }

    @Test
    fun calculatesNumberOfEventsInLastHourDividedBy60() {
        val eventName = "eventName"

        setMockedClockInstantSeconds(1)

        eventsStatisticImpl.incEvent(eventName)

        setMockedClockInstantSeconds(10)
        eventsStatisticImpl.incEvent(eventName)

        setMockedClockInstantSeconds(20)
        eventsStatisticImpl.incEvent(eventName)

        setMockedClockInstantSeconds(30)
        assertEquals(3.0 / 60, eventsStatisticImpl.getEventStatisticByName(eventName))

        setMockedClockInstantSeconds(Duration.ofHours(1).seconds)
        assertEquals(3.0 / 60, eventsStatisticImpl.getEventStatisticByName(eventName))

        setMockedClockInstantSeconds(1 + Duration.ofHours(1).seconds)
        assertEquals(2.0 / 60, eventsStatisticImpl.getEventStatisticByName(eventName))

        setMockedClockInstantSeconds(9 + Duration.ofHours(1).seconds)
        assertEquals(2.0 / 60, eventsStatisticImpl.getEventStatisticByName(eventName))

        setMockedClockInstantSeconds(10 + Duration.ofHours(1).seconds)
        assertEquals(1.0 / 60, eventsStatisticImpl.getEventStatisticByName(eventName))

        setMockedClockInstantSeconds(19 + Duration.ofHours(1).seconds)
        assertEquals(1.0 / 60, eventsStatisticImpl.getEventStatisticByName(eventName))

        setMockedClockInstantSeconds(20 + Duration.ofHours(1).seconds)
        assertEquals(0.0 / 60, eventsStatisticImpl.getEventStatisticByName(eventName))
    }

    @Test
    fun testPrintStatistic() {
        val outContent = ByteArrayOutputStream()
        PrintStream(outContent).use { printStream ->
            System.setOut(printStream)

            val eventName1 = "eventName1"
            val eventName2 = "eventName2"

            setMockedClockInstantSeconds(1)

            eventsStatisticImpl.incEvent(eventName1)

            setMockedClockInstantSeconds(10)
            eventsStatisticImpl.incEvent(eventName1)
            eventsStatisticImpl.incEvent(eventName2)

            setMockedClockInstantSeconds(1 + Duration.ofHours(1).seconds)
            eventsStatisticImpl.incEvent(eventName2)
            eventsStatisticImpl.printStatistic()

            assertEquals(
                """
                    eventName1 | total = 2 | in last hour = 1
                    eventName2 | total = 2 | in last hour = 2

                """.trimIndent(),
                outContent.toString()
            )
        }
        System.setOut(System.out)
    }
}
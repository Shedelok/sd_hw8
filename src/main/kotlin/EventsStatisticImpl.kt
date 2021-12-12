import java.time.Clock
import java.time.Duration
import java.time.Instant

private const val MINUTES_IN_HOUR = 60

class EventsStatisticImpl(private val clock: Clock) : EventsStatistic {
    private val eventsInLastHour = ArrayDeque<Event>()
    private val requestsCount = mutableMapOf<String, EventCount>()

    private fun cleanUp() {
        val timeHourAgo = clock.instant().minusSeconds(Duration.ofHours(1).seconds)
        while (eventsInLastHour.isNotEmpty()) {
            if (eventsInLastHour.first().instant.isAfter(timeHourAgo)) {
                break
            }
            val name = eventsInLastHour.removeFirst().name
            requestsCount.compute(name) { _, eventCount ->
                eventCount!!.copy(inLastHour = eventCount.inLastHour - 1)
            }
        }
    }

    override fun incEvent(name: String) {
        cleanUp()
        eventsInLastHour.add(Event(clock.instant(), name))
        requestsCount.compute(name) { _, count ->
            count?.copy(
                total = count.total + 1,
                inLastHour = count.inLastHour + 1
            ) ?: EventCount(1, 1)
        }
    }

    // for the last hour
    override fun getEventStatisticByName(name: String): Double {
        cleanUp()
        return requestsCount[name]?.inLastHour?.toDouble()?.div(MINUTES_IN_HOUR) ?: 0.0
    }

    // for the last hour, with zeros
    override fun getAllEventStatistic(): List<Pair<String, Double>> {
        cleanUp()
        return requestsCount.map { (name, count) -> name to count.inLastHour.toDouble() / 60 }
    }

    // for all the time
    override fun printStatistic() {
        cleanUp()
        for ((name, count) in requestsCount) {
            println("$name | total = ${count.total} | in last hour = ${count.inLastHour}")
        }
    }
}

private data class Event(val instant: Instant, val name: String)

private data class EventCount(val total: Int, val inLastHour: Int)
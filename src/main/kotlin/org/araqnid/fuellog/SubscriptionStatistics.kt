package org.araqnid.fuellog

import org.araqnid.appstatus.OnStatusPage
import org.araqnid.appstatus.StatusReport
import org.araqnid.eventstore.Position
import org.araqnid.eventstore.subscription.PollingEventSubscriptionService
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionStatistics @Inject constructor(private val clock: Clock)
    : PollingEventSubscriptionService.SubscriptionListener {

    val expectedPollInterval: Duration = Duration.ofSeconds(1)

    data class State(val lastPollFinished: PositionObserved? = null,
                     val lastEventRead: PositionObserved? = null)

    private var state: State = State()

    @Synchronized override fun pollFinished(position: Position, eventsRead: Int) {
        val now = now(position)
        if (eventsRead > 0) {
            state = state.copy(lastEventRead = now)
        }
        state = state.copy(lastPollFinished = now)
    }

    @OnStatusPage(label = "Last event read")
    @Synchronized fun lastEventRead(): String = with(state) {
        when (lastEventRead) {
            null -> "No events"
            else -> "Last event read: ${lastEventRead.position} at ${lastEventRead.instant}"
        }
    }

    @OnStatusPage(label = "Last event poll")
    @Synchronized fun lastPollFinished(): StatusReport = with(state) {
        when (lastPollFinished) {
            null -> StatusReport(StatusReport.Priority.WARNING, "Event reader not polled")
            else -> {
                val elapsed = Duration.between(lastPollFinished.instant, Instant.now(clock))
                val priority = when {
                    elapsed < expectedPollInterval.multipliedBy(3) -> StatusReport.Priority.OK
                    elapsed < expectedPollInterval.multipliedBy(8) -> StatusReport.Priority.WARNING
                    else -> StatusReport.Priority.CRITICAL
                }
                StatusReport(priority, "Last polled: ${lastPollFinished.position} at ${lastPollFinished.instant}")
            }
        }
    }

    private fun now(position: Position) = PositionObserved(position, Instant.now(clock))

    data class PositionObserved(val position: Position, val instant: Instant)
}
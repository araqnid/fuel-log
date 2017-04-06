package org.araqnid.fuellog

import org.araqnid.appstatus.OnStatusPage
import org.araqnid.eventstore.Position
import org.araqnid.eventstore.subscription.SnapshotEventSubscriptionService
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotStatistics @Inject constructor(private val clock: Clock)
    : SnapshotEventSubscriptionService.SubscriptionListener {

    data class State(val lastWroteSnapshot: PositionObserved? = null,
                     val lastLoadedSnapshot: PositionObserved? = null)

    private var state: State = State()

    @Synchronized override fun loadedSnapshot(position: Position) {
        state = state.copy(lastLoadedSnapshot = now(position))
    }

    @Synchronized override fun wroteSnapshot(position: Position) {
        state = state.copy(lastWroteSnapshot = now(position))
    }

    @OnStatusPage(label = "Last snapshot")
    @Synchronized fun lastSnapshot(): String = with(state) {
        when {
            lastWroteSnapshot != null -> "Last snapshot written: ${lastWroteSnapshot.position} at ${lastWroteSnapshot.instant}"
            lastLoadedSnapshot != null -> "Snapshot loaded: ${lastLoadedSnapshot.position} at ${lastLoadedSnapshot.instant}"
            else -> "No snapshots used or written"
        }
    }

    private fun now(position: Position) = PositionObserved(position, Instant.now(clock))

    data class PositionObserved(val position: Position, val instant: Instant)
}
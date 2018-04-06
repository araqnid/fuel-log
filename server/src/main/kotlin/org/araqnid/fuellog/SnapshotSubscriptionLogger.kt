package org.araqnid.fuellog

import com.google.common.base.Stopwatch
import org.araqnid.eventstore.Position
import org.araqnid.eventstore.subscription.SnapshotEventSubscriptionService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeUnit

class SnapshotSubscriptionLogger : SnapshotEventSubscriptionService.SubscriptionListener {
    private val logger = LoggerFactory.getLogger(SnapshotSubscriptionLogger::class.java)

    private val initialReplayStopwatch = Stopwatch.createUnstarted()
    private val snapshotLoadStopwatch = Stopwatch.createUnstarted()
    @Volatile private var doneInitialReplay: Boolean = false
    @Volatile private var initialSnapshotPosition: Position? = null

    override fun loadingSnapshot() {
        snapshotLoadStopwatch.start()
    }

    override fun loadedSnapshot(position: Position) {
        snapshotLoadStopwatch.stop()
        initialSnapshotPosition = position
    }

    override fun pollStarted(position: Position) {
        if (!doneInitialReplay) {
            initialReplayStopwatch.start()
        }
    }

    override fun pollFinished(position: Position, eventsRead: Int) {
        if (doneInitialReplay)
            return
        initialReplayStopwatch.stop()
        doneInitialReplay = true
        if (eventsRead > 0) {
            if (initialSnapshotPosition != null) {
                val duration = initialReplayStopwatch.duration + snapshotLoadStopwatch.duration
                logger.info("Played {} events after snapshot in {}; now at {}", eventsRead, duration, position)
            } else {
                val duration = initialReplayStopwatch.duration
                logger.info("Initialised from {} events in {}; now at {}", eventsRead, duration, position)
            }
        } else if (initialSnapshotPosition != null) {
            val duration = snapshotLoadStopwatch.duration
            logger.info("Initialised from snapshot in {}", duration)
        } else {
            logger.info("Starting with no events")
        }
    }

    override fun wroteSnapshot(position: Position) {
        logger.info("Wrote snapshot to position {}", position)
    }

    private val Stopwatch.duration: Duration
        get() = Duration.ofNanos(elapsed(TimeUnit.NANOSECONDS))
}
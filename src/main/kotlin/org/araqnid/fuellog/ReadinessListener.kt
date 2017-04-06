package org.araqnid.fuellog

import org.araqnid.appstatus.Readiness
import org.araqnid.eventstore.Position
import org.araqnid.eventstore.subscription.PollingEventSubscriptionService
import javax.inject.Singleton

@Singleton
class ReadinessListener : PollingEventSubscriptionService.SubscriptionListener {
    @Volatile
    var readiness: Readiness = Readiness.NOT_READY

    override fun pollFinished(position: Position, eventsRead: Int) {
        readiness = Readiness.READY
    }
}

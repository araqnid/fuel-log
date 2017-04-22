package org.araqnid.fuellog

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Path("/user/preferences")
class PreferencesResources {
    @GET
    @Produces("application/json")
    fun fetchPreferences() = PreferencesData(PreferencesData.VolumeUnit.LITRES, PreferencesData.DistanceUnit.MILES, "GBP")

    data class PreferencesData(val fuelVolumeUnit: VolumeUnit, val distanceUnit: DistanceUnit, val currency: String) {
        enum class VolumeUnit { LITRES, GALLONS }
        enum class DistanceUnit { MILES, KM }
    }
}

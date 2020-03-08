import kotlin.reflect.full.memberProperties

object LibraryVersions {
    const val jetty = "9.4.27.v20200227"
    const val jackson = "2.10.3"
    const val resteasy = "3.11.0.Final"
    const val guice = "4.2.2"
    const val guava = "28.2-jre"
    const val kotlinCoroutines = "1.3.4"
    const val appStatus = "0.1.5"
    const val slf4j = "1.7.30"
    const val eventstore = "0.0.24"

    fun toMap() =
            LibraryVersions::class.memberProperties
                    .associate { prop -> prop.name to prop.getter.call() as String }
}

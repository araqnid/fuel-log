import kotlin.reflect.full.memberProperties

object LibraryVersions {
    const val jetty = "9.4.20.v20190813"
    const val jackson = "2.9.9"
    const val resteasy = "3.1.4.Final"
    const val guice = "4.2.2"
    const val guava = "28.1-jre"
    const val kotlinCoroutines = "1.3.1"
    const val appStatus = "0.1.2"

    fun toMap() =
            LibraryVersions::class.memberProperties
                    .associate { prop -> prop.name to prop.getter.call() as String }
}
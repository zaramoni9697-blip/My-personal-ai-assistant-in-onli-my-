package verify

import java.io.File

/**
 * Entry point of the JARVIS core verification suite.
 *
 * Compiles the platform-independent Kotlin core of the Android app and exercises it on the
 * JVM: JSON engine, language detection, persona/identity, device-intent parsing, crypto,
 * provider registry, every provider adapter and real HTTP round trips.
 */
fun main(args: Array<String>) {
    val repoRoot = File(args.getOrElse(0) { System.getProperty("user.dir") }).absoluteFile
    val providersJson = File(repoRoot, "shared/providers.json")

    println("JARVIS core verification suite")
    println("repo root: $repoRoot")

    CoreChecks.run(providersJson.path)
    ProviderChecks.run(providersJson.path)
    IntegrationChecks.run(providersJson.path)

    TestKit.exitWithReport()
}

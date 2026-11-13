package verify

import kotlin.system.exitProcess

/** Minimal assertion helpers so the verification suite has zero external dependencies. */
object TestKit {

    private var passed = 0
    private val failures = ArrayList<String>()
    private var currentSuite = ""

    fun suite(name: String) {
        currentSuite = name
        println("\n=== $name ===")
    }

    fun check(condition: Boolean, description: String) {
        if (condition) {
            passed++
            println("  PASS  $description")
        } else {
            failures.add("$currentSuite :: $description")
            println("  FAIL  $description")
        }
    }

    fun <T> checkEquals(expected: T, actual: T, description: String) {
        val ok = expected == actual
        if (ok) {
            passed++
            println("  PASS  $description")
        } else {
            failures.add("$currentSuite :: $description (expected=$expected actual=$actual)")
            println("  FAIL  $description")
            println("        expected: $expected")
            println("        actual:   $actual")
        }
    }

    fun checkContains(haystack: String, needle: String, description: String) {
        check(haystack.contains(needle), "$description  (looked for '$needle' in: ${haystack.take(400)})")
    }

    fun report(): Int {
        println("\n----------------------------------------")
        println("PASSED: $passed   FAILED: ${failures.size}")
        if (failures.isNotEmpty()) {
            println("\nFailures:")
            failures.forEach { println(" - $it") }
        }
        println("----------------------------------------")
        return if (failures.isEmpty()) 0 else 1
    }

    fun exitWithReport() {
        exitProcess(report())
    }
}

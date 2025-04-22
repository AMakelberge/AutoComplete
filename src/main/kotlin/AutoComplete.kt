package autocomplete

import java.io.File
import java.util.jar.JarFile
import kotlin.math.min

fun getLibs(): Map<String, String> {
    val libs = mutableMapOf<String, String>()
    val classPath = System.getProperty("java.class.path")
    val entries = classPath.split(File.pathSeparatorChar)

    for (entry in entries) {
        if (entry.contains("kotlin-stdlib")) {
            JarFile(entry).use { jar ->
                jar.entries().asSequence()
                    // only kotlin packages
                    .filter { it.name.startsWith("kotlin/") }
                    .forEach { jarEntry ->
                        val path = jarEntry.name
                        when {
                            // .kt source files: read the text
                            path.endsWith(".kt") -> {
                                val fqName = path
                                    .removeSuffix(".kt")
                                    .replace('/', '.')
                                val src = jar.getInputStream(jarEntry)
                                    .bufferedReader()
                                    .use { it.readText() }
                                libs[fqName] = src
                            }
                            // .class files: just register the name, leave source blank
                            path.endsWith(".class")
                                    && !path.contains("Kt__")
                                    && !path.contains("$") -> {
                                val fqName = path
                                    .removeSuffix(".class")
                                    .replace('/', '.')
                                libs.putIfAbsent(fqName, "")
                            }
                            else -> {
                                // ignore everything else
                            }
                        }
                        // for both .kt and .class, also register parent packages
                        if (path.endsWith(".class") || path.endsWith(".kt")) {
                            var dotIdx = jarEntry.name
                                .substringBeforeLast('.', "")
                                .replace('/', '.')
                                .lastIndexOf('.')
                            while (dotIdx > 0) {
                                val pkg = jarEntry.name
                                    .substringBeforeLast('.', "")
                                    .replace('/', '.')
                                    .substring(0, dotIdx)
                                libs.putIfAbsent(pkg, "")
                                dotIdx = pkg.lastIndexOf('.')
                            }
                        }
                    }
            }
        }
    }
    return libs
}

// Gets all kotlin standard-library libraries
fun getLibsOld() : Set<String> {
    val libs = mutableSetOf<String>()
    val path = System.getProperty("java.class.path")
    val entries = path.split(File.pathSeparator)

    // Go through every possible path and check if it's in kotlin-stdlib and format it into style of import
    for (entry in entries) {
        if (entry.contains("kotlin-stdlib")) {
            val file = JarFile(entry)
            for (jarEntry in file.entries()) {
                val name = jarEntry.name
                if (name.endsWith(".class") && name.startsWith("kotlin")) {
                    val libName = name.removeSuffix(".class").replace("/", ".")
                    if (!name.contains("Kt__") && !name.contains("$")){
                        libs.add(libName)
                        // Allows for parent libraries to be added
                        // eg: kotlin.collections included if kotlin.collections.AbstractCollection added
                        var dotIndex = libName.lastIndexOf('.')
                        while (dotIndex >= 0) {
                            val parentPackage = libName.substring(0, dotIndex)
                            libs.add(parentPackage)
                            dotIndex = parentPackage.lastIndexOf('.')
                    }
                    }
                }
            }
        }
    }
    return libs
}

// Fun gives closest library based off query and existant libraries
fun matchLibrary(query: String, libs: List<String>): Map<String, Double> {
    val queryLow = query.lowercase()

//    // Exact match check
//    libs.firstOrNull { it.equals(query, ignoreCase = true) }?.let { exactMatch ->
//        return exactMatch
//    }

    // Helper to calculate levenshtein distance for non-exact matches
    fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i-1] == b[j-1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i-1][j] + 1, dp[i][j-1] + 1),
                    dp[i-1][j-1] + cost
                )
            }
        }
        return dp[m][n]
    }

    // Make candidates and give each a score and get the best one
    val candidates = mutableMapOf<String, Double>()
    for (lib in libs) {
        val libLow = lib.lowercase()
        val dist = levenshtein(queryLow, libLow)
        val maxLen = maxOf(queryLow.length, libLow.length)
        // Prioritizes shorter/simpler imports
        var similarity = 1.0 - (dist.toDouble() / maxLen.toDouble())

        if (libLow.contains(queryLow)) {
            // Very good if library contains query
            similarity += 1.0
        }
        // Values don't need to be capped as they are relative to each other

        candidates[lib] = similarity
    }
    return candidates//maxByOrNull { it.value }?.key ?: "Nothing found"
}

fun main(args: Array<String>) {
    val query = "class" //args[0]
    val libs = getLibs()
    libs.forEach { (name, src) ->
        if (src.contains(query)) {
            println("Found in $name.kt:\n$src")
        }
    }
}
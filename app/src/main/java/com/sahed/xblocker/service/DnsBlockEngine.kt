package com.sahed.xblocker.service

/**
 * DNS block engine: O(1) HashSet lookup with wildcard support.
 * Thread-safe via @Volatile — reads are safe from any thread.
 */
class DnsBlockEngine {

    @Volatile
    private var blocklist: HashSet<String> = hashSetOf()

    @Volatile
    private var wildcards: List<String> = emptyList()

    /**
     * Load domains into the engine. Call on a background thread.
     * Accepts both exact domains ("example.com") and wildcard patterns ("*.example.com" stored as "_.example.com").
     */
    fun loadDomains(domains: Collection<String>) {
        val exact = HashSet<String>(domains.size * 2)
        val wild = mutableListOf<String>()

        for (raw in domains) {
            val d = raw.trim().lowercase()
            if (d.isBlank() || d.startsWith("#")) continue
            when {
                d.startsWith("*.") -> wild.add(d.removePrefix("*."))
                d.startsWith("_.") -> wild.add(d.removePrefix("_."))
                else -> exact.add(d)
            }
        }

        blocklist = exact
        wildcards = wild
    }

    /**
     * Check if a domain should be blocked.
     * Returns true if domain is in blocklist or matches a wildcard suffix.
     */
    fun shouldBlock(domain: String): Boolean {
        if (domain.isBlank()) return false
        val normalized = domain.lowercase().trimEnd('.')

        // Exact match first (O(1))
        if (blocklist.contains(normalized)) return true

        // Wildcard suffix match
        for (wildcard in wildcards) {
            if (normalized == wildcard || normalized.endsWith(".$wildcard")) return true
        }

        return false
    }

    fun size(): Int = blocklist.size + wildcards.size

    fun clear() {
        blocklist = hashSetOf()
        wildcards = emptyList()
    }
}

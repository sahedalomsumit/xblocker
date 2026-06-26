package com.sahed.xblocker.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DnsBlockEngineTest {

    private lateinit var engine: DnsBlockEngine

    @Before
    fun setup() {
        engine = DnsBlockEngine()
        engine.loadDomains(
            listOf(
                "pornhub.com",
                "xvideos.com",
                "*.xxx",
                "_.badsite.com",
                "# comment line",
                "  spaced.com  "
            )
        )
    }

    @Test
    fun `exact domain is blocked`() {
        assertTrue(engine.shouldBlock("pornhub.com"))
        assertTrue(engine.shouldBlock("xvideos.com"))
    }

    @Test
    fun `exact domain case-insensitive`() {
        assertTrue(engine.shouldBlock("PornHub.Com"))
        assertTrue(engine.shouldBlock("XVIDEOS.COM"))
    }

    @Test
    fun `trailing dot stripped`() {
        assertTrue(engine.shouldBlock("pornhub.com."))
    }

    @Test
    fun `non-blocked domain passes`() {
        assertFalse(engine.shouldBlock("google.com"))
        assertFalse(engine.shouldBlock("youtube.com"))
    }

    @Test
    fun `wildcard star xxx blocks subdomains`() {
        assertTrue(engine.shouldBlock("whatever.xxx"))
        assertTrue(engine.shouldBlock("deep.sub.xxx"))
    }

    @Test
    fun `underscore wildcard blocks subdomains of badsite`() {
        assertTrue(engine.shouldBlock("sub.badsite.com"))
        assertTrue(engine.shouldBlock("deep.sub.badsite.com"))
    }

    @Test
    fun `comment lines ignored`() {
        assertFalse(engine.shouldBlock("# comment line"))
    }

    @Test
    fun `whitespace trimmed from domains`() {
        assertTrue(engine.shouldBlock("spaced.com"))
    }

    @Test
    fun `empty domain not blocked`() {
        assertFalse(engine.shouldBlock(""))
        assertFalse(engine.shouldBlock("   "))
    }

    @Test
    fun `clear resets engine`() {
        engine.clear()
        assertFalse(engine.shouldBlock("pornhub.com"))
    }
}

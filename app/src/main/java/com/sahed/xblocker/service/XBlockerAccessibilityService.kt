package com.sahed.xblocker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.sahed.xblocker.data.datastore.AppPreferences
import com.sahed.xblocker.data.repository.BlocklistRepository
import com.sahed.xblocker.ui.BlockedActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Accessibility Service that monitors browser address bars in real-time.
 * When a blocked domain is detected in the URL, it navigates back and
 * launches BlockedActivity to inform the user.
 *
 * Supported browsers (by package name):
 *   Chrome, Brave, Samsung Internet, Firefox, Opera, Edge, DuckDuckGo
 */
@AndroidEntryPoint
class XBlockerAccessibilityService : AccessibilityService() {

    @Inject lateinit var blocklistRepo: BlocklistRepository
    @Inject lateinit var preferences: AppPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val blockEngine = DnsBlockEngine()

    @Volatile private var lastBlockedDomain: String = ""
    @Volatile private var lastBlockedTs: Long = 0L

    // Lock object for atomic debounce check-and-set
    private val debounceLock = Any()

    companion object {
        private const val TAG = "XBlockerA11y"

        /** Debounce: don't re-trigger a full block for the same domain within this window */
        private const val BLOCK_DEBOUNCE_MS = 3000L

        /** During debounce window, keep pushing BACK so user can't scroll the page */
        private const val BACK_REPEAT_MS = 1000L

        /** Packages known to expose their URL address bar via accessibility */
        val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "org.chromium.chrome",           // Chromium
            "com.brave.browser",
            "com.sec.android.app.sbrowser",  // Samsung Internet
            "org.mozilla.firefox",
            "org.mozilla.fenix",             // Firefox Fenix
            "com.opera.browser",
            "com.opera.mini.native",
            "com.microsoft.emmx",            // Edge
            "com.duckduckgo.mobile.android",
            "com.vivaldi.browser",
            "com.kiwibrowser.browser",
        )

        /** Check if the accessibility service is currently running */
        fun isRunning(context: Context): Boolean {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                    as android.view.accessibility.AccessibilityManager
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(
                "${context.packageName}/${XBlockerAccessibilityService::class.java.name}"
            )
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceScope.launch {
            preferences.setBlockerActive(true)
            combine(
                blocklistRepo.getAllDomains(),
                preferences.isDefaultListEnabled
            ) { _, _ ->
                loadBlocklist()
            }.collect {}
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in BROWSER_PACKAGES) return

        serviceScope.launch(Dispatchers.Main) {
            // Check if blocking is enabled
            val isActive = preferences.isBlockerActive.first()
            if (!isActive) return@launch

            val url = extractUrlFromEvent(event) ?: return@launch
            val domain = parseDomain(url) ?: return@launch

            if (blockEngine.shouldBlock(domain)) {
                val now = System.currentTimeMillis()
                val timeSinceLastBlock = now - lastBlockedTs

                // During the debounce window: keep pressing BACK to prevent scrolling,
                // but don't re-launch the BlockedActivity overlay.
                if (domain == lastBlockedDomain && timeSinceLastBlock < BLOCK_DEBOUNCE_MS) {
                    if (timeSinceLastBlock > BACK_REPEAT_MS) {
                        Log.d(TAG, "Re-pressing BACK to keep $domain blocked")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                    return@launch
                }

                // Atomic check-and-set to prevent duplicate launches from concurrent events
                val shouldLaunch = synchronized(debounceLock) {
                    val nowInner = System.currentTimeMillis()
                    if (domain == lastBlockedDomain && nowInner - lastBlockedTs < BLOCK_DEBOUNCE_MS) {
                        false
                    } else {
                        lastBlockedDomain = domain
                        lastBlockedTs = nowInner
                        true
                    }
                }
                if (!shouldLaunch) return@launch

                Log.i(TAG, "BLOCKED: $domain (from $pkg)")
                blockDomain(domain)
                serviceScope.launch {
                    blocklistRepo.incrementBlockedCount(domain)
                    preferences.incrementBlockedToday()
                }
            }
        }
    }

    /**
     * Navigate away from the blocked page and show the BlockedActivity overlay.
     */
    private fun blockDomain(domain: String) {
        // Press Back to leave the blocked page
        performGlobalAction(GLOBAL_ACTION_BACK)

        // Launch the overlay activity
        // FLAG_ACTIVITY_SINGLE_TOP ensures that if BlockedActivity is already on top,
        // it won't be created again (preventing double popup).
        val intent = Intent(this, BlockedActivity::class.java).apply {
            putExtra(BlockedActivity.EXTRA_DOMAIN, domain)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        startActivity(intent)
    }

    /**
     * Traverse the accessibility node tree to find the browser address bar text.
     * Browsers typically use view IDs like "url_bar", "address_bar", "search_box", etc.
     * Falls back to traversal of TYPE_EDIT_TEXT nodes in the window.
     */
    private fun extractUrlFromEvent(event: AccessibilityEvent): String? {
        val source = event.source ?: return null
        return try {
            findUrlInNode(source)
        } finally {
            source.recycle()
        }
    }

    private fun findUrlInNode(root: AccessibilityNodeInfo): String? {
        // First: try known view IDs
        val knownIds = listOf(
            "com.android.chrome:id/url_bar",
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            "org.mozilla.fenix:id/mozac_browser_toolbar_url_view",
            "com.brave.browser:id/url_bar",
            "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "com.microsoft.emmx:id/url_bar",
            "com.duckduckgo.mobile.android:id/omnibarTextInput",
            "com.opera.browser:id/url_field",
            "com.vivaldi.browser:id/url_bar",
        )

        for (viewId in knownIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
            if (nodes.isNotEmpty()) {
                val text = nodes[0].text?.toString()
                nodes.forEach { it.recycle() }
                if (!text.isNullOrBlank()) return text
            }
        }

        // Fallback: BFS for any editable node that looks like a URL
        return findUrlByTraversal(root)
    }

    private fun findUrlByTraversal(node: AccessibilityNodeInfo): String? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(node)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val text = current.text?.toString()
            if (current.className == "android.widget.EditText" && !text.isNullOrBlank()) {
                if (looksLikeUrl(text)) return text
            }
            for (i in 0 until current.childCount) {
                current.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun looksLikeUrl(text: String): Boolean {
        val lower = text.lowercase()
        return lower.startsWith("http://") ||
                lower.startsWith("https://") ||
                lower.contains(".") && !lower.contains(" ") && lower.length > 4
    }

    /**
     * Extract the hostname from a raw URL or plain domain string.
     */
    private fun parseDomain(raw: String): String? {
        return try {
            val withScheme = if (!raw.startsWith("http")) "https://$raw" else raw
            val uri = java.net.URI(withScheme)
            val host = uri.host?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
            // Strip leading www. so "www.example.com" matches "example.com" in the blocklist
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun loadBlocklist() {
        val customDomains = blocklistRepo.getAllActiveDomains()
        val isDefaultEnabled = preferences.isDefaultListEnabled.first()
        val allDomains = mutableListOf<String>()
        allDomains.addAll(customDomains)

        if (isDefaultEnabled) {
            try {
                assets.open("blocklist.txt").bufferedReader().use { reader ->
                    allDomains.addAll(reader.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") })
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not load default blocklist: ${e.message}")
            }
        }

        blockEngine.loadDomains(allDomains)
        Log.i(TAG, "Blocklist loaded: ${blockEngine.size()} domains")
    }

    /** Called by external code (e.g. after blocklist changes) to reload. */
    fun reloadBlocklist() {
        serviceScope.launch { loadBlocklist() }
    }

    override fun onInterrupt() {
        Log.i(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch { preferences.setBlockerActive(false) }
        serviceScope.cancel()
    }
}

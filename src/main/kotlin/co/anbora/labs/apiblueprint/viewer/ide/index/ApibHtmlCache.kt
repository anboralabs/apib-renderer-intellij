package co.anbora.labs.apiblueprint.viewer.ide.index

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Global cache for rendered HTML content of API Blueprint (.apib) files.
 * The cache uses content hash as key to detect file changes.
 */
@Service(Service.Level.APP)
class ApibHtmlCache {

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    /**
     * Gets the cached HTML for the given file path and content.
     * Returns null if the cache is invalid or doesn't exist.
     *
     * @param filePath The path of the apib file
     * @param content The current content of the file
     * @return The cached HTML string or null if not cached or content changed
     */
    fun getHtml(filePath: String, content: String): String? {
        val contentHash = computeHash(content)
        val entry = cache[filePath]
        
        return if (entry != null && entry.contentHash == contentHash) {
            entry.html
        } else {
            null
        }
    }

    /**
     * Stores the rendered HTML in the cache.
     *
     * @param filePath The path of the apib file
     * @param content The content of the file (used to compute hash)
     * @param html The rendered HTML string
     */
    fun putHtml(filePath: String, content: String, html: String) {
        val contentHash = computeHash(content)
        cache[filePath] = CacheEntry(contentHash, html)
    }

    /**
     * Invalidates the cache entry for the given file path.
     *
     * @param filePath The path of the apib file to invalidate
     */
    fun invalidate(filePath: String) {
        cache.remove(filePath)
    }

    /**
     * Clears all cached entries.
     */
    fun clearAll() {
        cache.clear()
    }

    /**
     * Checks if there is a valid cache entry for the given file and content.
     *
     * @param filePath The path of the apib file
     * @param content The current content of the file
     * @return true if a valid cache entry exists
     */
    fun isCached(filePath: String, content: String): Boolean {
        val contentHash = computeHash(content)
        val entry = cache[filePath]
        return entry != null && entry.contentHash == contentHash
    }

    /**
     * Computes MD5 hash of the content string.
     */
    private fun computeHash(content: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(content.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Cache entry containing the content hash and rendered HTML.
     */
    private data class CacheEntry(
        val contentHash: String,
        val html: String
    )

    companion object {
        @JvmStatic
        fun getInstance(): ApibHtmlCache = service()
    }
}

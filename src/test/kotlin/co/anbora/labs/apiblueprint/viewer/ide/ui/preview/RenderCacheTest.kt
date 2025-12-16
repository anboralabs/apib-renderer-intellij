package co.anbora.labs.apiblueprint.viewer.ide.ui.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RenderCacheTest {

    private fun computeHtml(content: String, settingsSignature: String): String =
        markdownToHtml(content, settingsSignature, css = null)

    @Test
    fun `cache hit when content and settings unchanged`() {
        val cache = RenderCache()
        val key = CacheKey("file://x.md", "dark|safe")
        val content = "Hello"
        val h1 = sha256("${key.settingsSignature}:::$content")
        val html1 = computeHtml(content, key.settingsSignature)
        cache.put(key, CacheEntry(h1, html1))

        // simulate lookup
        val entry = cache.get(key)
        requireNotNull(entry)
        assertEquals(h1, entry.lastHash)
        assertEquals(html1, entry.lastHtml)
    }

    @Test
    fun `cache miss when content changes`() {
        val cache = RenderCache()
        val key = CacheKey(null, "light|safe")

        val content1 = "A"
        val hash1 = sha256("${key.settingsSignature}:::$content1")
        val html1 = computeHtml(content1, key.settingsSignature)
        cache.put(key, CacheEntry(hash1, html1))

        val content2 = "B" // changed
        val hash2 = sha256("${key.settingsSignature}:::$content2")
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `invalidates when settings change`() {
        val cache = RenderCache()
        val key = CacheKey("vfile:///a.md", "light")
        cache.put(key, CacheEntry("h", "html"))
        cache.clearForSettingsSignature("light")
        val e = cache.get(key)
        // entry should be gone
        assertEquals(null, e)
    }

    @Test
    fun `basic thread safety under concurrency`() {
        val cache = RenderCache()
        val key = CacheKey("file:///t.md", "s1")

        val pool = Executors.newFixedThreadPool(4)
        val latch = CountDownLatch(20)
        repeat(20) {
            pool.submit {
                val hash = sha256("${key.settingsSignature}:::content-$it")
                cache.put(key, CacheEntry(hash, "html-$it"))
                latch.countDown()
            }
        }
        latch.await(2, TimeUnit.SECONDS)
        pool.shutdown()

        // last write wins, but structure must remain consistent
        val e = cache.get(key)
        requireNotNull(e)
        assertEquals(true, e.lastHtml.startsWith("html-"))
        assertEquals(64, e.lastHash.length) // sha-256 hex
    }
}

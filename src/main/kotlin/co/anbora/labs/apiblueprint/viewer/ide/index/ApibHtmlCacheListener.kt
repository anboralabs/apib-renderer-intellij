package co.anbora.labs.apiblueprint.viewer.ide.index

import com.intellij.util.messages.Topic
import java.util.*

/**
 * Listener interface for API Blueprint HTML cache updates.
 *
 * This listener is notified when new HTML content is cached for an API Blueprint file,
 * allowing components like editors to refresh their displays.
 */
fun interface ApibHtmlCacheListener : EventListener {

    /**
     * Called when new HTML content has been cached for a file.
     *
     * @param filePath The path of the .apib file whose HTML was cached
     * @param html The cached HTML content
     */
    fun onHtmlCached(filePath: String, html: String)

    companion object {
        /**
         * Topic for subscribing to HTML cache update events.
         *
         * Subscribe using: ApplicationManager.getApplication().messageBus.connect()
         *   .subscribe(ApibHtmlCacheListener.TOPIC, listener)
         */
        @JvmField
        val TOPIC = Topic.create(
            "API Blueprint HTML Cache Updates",
            ApibHtmlCacheListener::class.java
        )
    }
}
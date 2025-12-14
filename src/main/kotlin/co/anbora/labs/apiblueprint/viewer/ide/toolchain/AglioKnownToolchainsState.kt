package co.anbora.labs.apiblueprint.viewer.ide.toolchain

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil
import java.util.concurrent.Flow

@State(
    name = "Aglio Home",
    storages = [Storage("7591d164-556f-49eb-95a3-e0cc2ca123a9_NewAglioToolchains.xml")]
)
class AglioKnownToolchainsState: PersistentStateComponent<AglioKnownToolchainsState?> {
    companion object {
        fun getInstance() = service<AglioKnownToolchainsState>()
    }

    var knownToolchains: Set<String> = emptySet()

    @Volatile
    private var jMeterToolchains: MutableSet<AglioToolchain> = mutableSetOf()

    fun knownToolchains(): Set<AglioToolchain> = jMeterToolchains

//    @Volatile
//    private var toolchainPublisher = JMeterToolchainPublisher()

    fun isKnown(homePath: String): Boolean {
        return knownToolchains.contains(homePath)
    }

    fun add(toolchain: AglioToolchain) {
        knownToolchains = knownToolchains + toolchain.homePath()
        jMeterToolchains.add(toolchain)
//        toolchainPublisher.publish(jMeterToolchains)
    }

    fun remove(toolchain: AglioToolchain) {
        knownToolchains = knownToolchains - toolchain.homePath()
        jMeterToolchains.remove(toolchain)
//        toolchainPublisher.publish(jMeterToolchains)
    }

    fun subscribe(subscriber: Flow.Subscriber<Set<AglioToolchain>>) {
//        toolchainPublisher.subscribe(subscriber)
    }

    override fun getState() = this

    override fun loadState(state: AglioKnownToolchainsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun initializeComponent() {
        val app = ApplicationManager.getApplication()
        app.executeOnPooledThread {
            jMeterToolchains = knownToolchains.map { AglioToolchain.fromPath(it) }.toMutableSet()
//            toolchainPublisher.publish(jMeterToolchains)
        }.get()
    }
}

package co.anbora.labs.apiblueprint.viewer.ide.toolchain

import co.anbora.labs.apiblueprint.viewer.ide.utils.toPath
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Attribute

@State(
    name = "Aglio Toolchain",
    storages = [Storage("05cec02a-b679-40fc-b146-f3ec972eaae5_NewAglioHome.xml")]
)
class AglioToolchainService: PersistentStateComponent<AglioToolchainService.ToolchainState?> {
    private var state = ToolchainState()

    @Volatile
    private var toolchain: AglioToolchain = NullToolchain

    fun setToolchain(newToolchain: AglioToolchain) {
        toolchain = newToolchain
        state.toolchainLocation = newToolchain.homePath()
        state.toolchainVersion = newToolchain.version()
    }

    fun toolchain(): AglioToolchain = toolchain

    override fun getState() = state

    override fun loadState(state: ToolchainState) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    override fun initializeComponent() {
        val app = ApplicationManager.getApplication()
        app.executeOnPooledThread {
            val currentLocation = state.toolchainLocation
            val version = state.toolchainVersion
            if (toolchain == NullToolchain && currentLocation.isNotEmpty()) {
                setToolchain(AglioLocalToolchain(version, currentLocation.toPath()))
            }
        }.get()
    }

    companion object {
        val toolchainSettings
            get() = service<AglioToolchainService>()
    }

    class ToolchainState {
        @Attribute("location")
        var toolchainLocation: String = ""

        @Attribute("version")
        var toolchainVersion: String = ""
    }
}
package co.anbora.labs.apiblueprint.viewer.ide.discovery

import co.anbora.labs.apiblueprint.viewer.ide.settings.ConfigurationUtil
import co.anbora.labs.apiblueprint.viewer.ide.utils.toPathOrNull

object Discovery {

    val unixPath by lazy {
        ConfigurationUtil.executeAndReturnOutput("which", "aglio").toPathOrNull()
    }

    val windowsPath by lazy {
        ConfigurationUtil.executeAndReturnOutput("where", "aglio.exe").toPathOrNull()
    }

}

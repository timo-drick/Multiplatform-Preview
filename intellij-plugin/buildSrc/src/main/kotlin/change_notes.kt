import org.gradle.api.Project
import org.intellij.lang.annotations.Language

data class VersionInfo(
    val version: String,
    val items: MutableList<String> = mutableListOf()
)

@Language("HTML")
fun getChangeNotesText(rootProject: Project) = run {
    val releaseNotesFile = rootProject.file("RELEASE_NOTES_PLUGIN.md")
    val versionModel = mutableListOf<VersionInfo>()
    var currentVersion: VersionInfo? = null
    var versionCount = 4
    releaseNotesFile.readLines().forEach { line ->
        if (line.startsWith("### ") && versionCount > 0) {
            versionCount--
            if (versionCount > 0) {
                val version = line.substring(3).trim()
                currentVersion = VersionInfo(version).also {
                    versionModel.add(it)
                }
            }
        } else if (versionCount > 0 && line.startsWith("- ")) {
            currentVersion?.items?.add(line.substring(2).trim())
        }
    }

    val htmlBuilder = StringBuilder()
    versionModel.forEach { versionInfo ->
        htmlBuilder.append("<h3>${versionInfo.version}</h3>")
        htmlBuilder.append("<ul>")
        versionInfo.items.forEach { items ->
            htmlBuilder.append("<li>$items</li>")
        }
        htmlBuilder.append("</ul>")
    }
    htmlBuilder.toString()
}

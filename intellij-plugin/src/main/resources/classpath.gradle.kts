allprojects {
    tasks.register("hotPreviewDetectClasspath") {
        val classpathRuntime = project.configurations.findByName("jvmRuntimeClasspath")?.map { it.absolutePath }
        val outputFile = layout.buildDirectory.file("hotPreviewClasspath.txt")
        doLast {
            val text = classpathRuntime?.joinToString("\n")
            outputFile.get().asFile.writeText(text ?: "")
        }
    }
}
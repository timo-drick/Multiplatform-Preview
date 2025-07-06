allprojects {
    project.tasks.register("hotPreviewDetectClasspath") {
        group = "hotPreview"
        val targetName = try {
            val kotlinExt = project.extensions.findByName("kotlin")
            val targets = kotlinExt?.javaClass?.getMethod("getTargets")?.invoke(kotlinExt) as? Collection<*>
            targets?.forEach { println(it?.javaClass?.simpleName) }
            val jvmTarget = targets?.firstOrNull { it?.javaClass?.simpleName?.contains("KotlinJvmTarget") ?: false }
            jvmTarget?.javaClass?.getMethod("getName")?.invoke(jvmTarget) as? String ?: "jvm"
        } catch (err: Throwable) {
            "jvm"
        }
        val runtimeClasspath = project.configurations.findByName("${targetName}RuntimeClasspath")?.map { it.absolutePath }
        val compileClasspath = project.configurations.findByName("${targetName}CompileClasspath")?.map { it.absolutePath }
        //val allConfigurationNames = project.configurations.joinToString("\n") { it.name }

        val runtimeFile = layout.buildDirectory.file("hotPreviewRuntimeClasspath.txt")
        val compileFile = layout.buildDirectory.file("hotPreviewCompileClasspath.txt")
        //val configFile = layout.buildDirectory.file("hotPreviewConfigurations.txt")
        val jvmTargetName = layout.buildDirectory.file("hotPreviewJvmTargetName.txt")
        val buildDirectory = layout.buildDirectory
        doLast {
            buildDirectory.get().asFile.mkdirs()
            //configFile.get().asFile.writeText(allConfigurationNames)
            runtimeFile.get().asFile.writeText(runtimeClasspath?.joinToString("\n") ?: "")
            compileFile.get().asFile.writeText(compileClasspath?.joinToString("\n") ?: "")
            jvmTargetName.get().asFile.writeText(targetName)
        }
    }
}

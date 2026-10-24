package com.sprout.ide.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.sprout.ide.cloud.CollabMarker
import com.sprout.ide.lang.Languages
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ProjectInfo(val dir: File, val name: String, val modified: Long, val files: Int, val mainLanguage: String, val shared: Boolean = false)

enum class ProjectTemplate(val title: String, val subtitle: String, val languageId: String) {
    Empty("Пустой", "Только README", "markdown"),
    Web("Веб-сайт", "HTML + CSS + JS", "html"),
    Python("Python", "main.py", "python"),
    Kotlin("Kotlin", "Main.kt", "kotlin"),
    Java("Java", "Main.java", "java"),
    Node("JavaScript", "index.js + package.json", "javascript"),
    C("C", "main.c + Makefile", "c"),
    Cpp("C++", "main.cpp", "cpp"),
    Go("Go", "main.go + go.mod", "go"),
    Rust("Rust", "Cargo + src/main.rs", "rust"),
}

class Workspace(context: Context) {
    val root: File = File(context.filesDir, "projects").apply { mkdirs() }

    fun projects(): List<ProjectInfo> = root.listFiles { f -> f.isDirectory }.orEmpty()
        .map { dir ->
            val all = dir.walkTopDown().filter { it.isFile && it.name != CollabMarker.FILE }.toList()
            val modified = (all.maxOfOrNull { it.lastModified() } ?: dir.lastModified())
            val lang = all.groupingBy { Languages.forFile(it.name) }.eachCount()
                .filterKeys { it != Languages.Markdown && it != Languages.Plain && it != Languages.Json }
                .maxByOrNull { it.value }?.key ?: Languages.Markdown
            ProjectInfo(dir, dir.name, modified, all.size, lang.id, File(dir, CollabMarker.FILE).exists())
        }
        .sortedByDescending { it.modified }

    fun uniqueName(base: String): String {
        val clean = base.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "Проект" }
        if (!File(root, clean).exists()) return clean
        var i = 2
        while (File(root, "$clean $i").exists()) i++
        return "$clean $i"
    }

    fun createProject(name: String, template: ProjectTemplate): File {
        val dir = File(root, uniqueName(name)).apply { mkdirs() }
        val n = dir.name
        fun put(path: String, content: String) = File(dir, path).apply { parentFile?.mkdirs(); writeText(content) }
        put("README.md", "# $n\n\nСоздано в Sprout IDE.\n")
        when (template) {
            ProjectTemplate.Empty -> Unit
            ProjectTemplate.Web -> {
                put("index.html", Languages.Html.template("index.html"))
                put("style.css", Languages.Css.template("style.css"))
                put("script.js", "document.querySelector('h1').addEventListener('click', e => {\n  e.target.textContent = 'Sprout 🌱';\n  console.log('clicked!');\n});\n")
            }
            ProjectTemplate.Python -> put("main.py", Languages.Python.template("main.py"))
            ProjectTemplate.Kotlin -> put("Main.kt", Languages.Kotlin.template(n))
            ProjectTemplate.Java -> put("Main.java", Languages.Java.template("Main.java"))
            ProjectTemplate.Node -> {
                put("index.js", Languages.JavaScript.template("index.js"))
                put("package.json", "{\n  \"name\": \"${n.lowercase().replace(' ', '-')}\",\n  \"version\": \"1.0.0\",\n  \"main\": \"index.js\"\n}\n")
            }
            ProjectTemplate.C -> {
                put("main.c", Languages.C.template("main.c"))
                put("Makefile", "main: main.c\n\tcc -Wall -o main main.c\n")
            }
            ProjectTemplate.Cpp -> put("main.cpp", Languages.Cpp.template("main.cpp"))
            ProjectTemplate.Go -> {
                put("main.go", Languages.Go.template("main.go"))
                put("go.mod", "module ${n.lowercase().replace(' ', '-')}\n\ngo 1.22\n")
            }
            ProjectTemplate.Rust -> {
                put("src/main.rs", Languages.Rust.template("main.rs"))
                put("Cargo.toml", "[package]\nname = \"${n.lowercase().replace(' ', '_')}\"\nversion = \"0.1.0\"\nedition = \"2021\"\n")
            }
        }
        return dir
    }

    fun list(dir: File, s: AppSettings): List<File> {
        val items = dir.listFiles().orEmpty().filter { s.showHidden || !it.name.startsWith(".") }
        val sorted = when (s.fileSort) {
            FileSort.Name -> items.sortedBy { it.name.lowercase() }
            FileSort.Type -> items.sortedWith(compareBy({ it.extension.lowercase() }, { it.name.lowercase() }))
            FileSort.Modified -> items.sortedByDescending { it.lastModified() }
        }
        return if (s.foldersFirst) sorted.sortedByDescending { it.isDirectory } else sorted
    }

    fun createFile(parent: File, name: String, withTemplate: Boolean): File {
        val f = File(parent, name)
        f.parentFile?.mkdirs()
        if (!f.exists()) f.writeText(if (withTemplate) Languages.forFile(name).template(name) else "")
        return f
    }

    fun createFolder(parent: File, name: String): File = File(parent, name).apply { mkdirs() }

    fun rename(file: File, newName: String): File? {
        val target = File(file.parentFile, newName)
        return if (!target.exists() && file.renameTo(target)) target else null
    }

    fun delete(file: File) = file.deleteRecursively()

    fun duplicate(file: File): File {
        val base = file.nameWithoutExtension
        val ext = if (file.extension.isNotEmpty()) "." + file.extension else ""
        var i = 1
        var target: File
        do { target = File(file.parentFile, "$base copy${if (i > 1) " $i" else ""}$ext"); i++ } while (target.exists())
        file.copyRecursively(target)
        return target
    }

    fun importUri(resolver: ContentResolver, uri: Uri, intoDir: File): File? = runCatching {
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: "imported.txt"
        var target = File(intoDir, name)
        var i = 2
        while (target.exists()) { target = File(intoDir, "${name.substringBeforeLast('.')} ($i).${name.substringAfterLast('.', "txt")}"); i++ }
        resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
        if (name.endsWith(".zip", true)) {
            val unpackTo = File(intoDir, name.removeSuffix(".zip").removeSuffix(".ZIP")).apply { mkdirs() }
            unzip(target, unpackTo)
            target.delete()
            unpackTo
        } else target
    }.getOrNull()

    fun exportZip(resolver: ContentResolver, dir: File, uri: Uri): Boolean = runCatching {
        resolver.openOutputStream(uri)?.use { os ->
            ZipOutputStream(os).use { zip ->
                dir.walkTopDown().filter { it.isFile }.forEach { f ->
                    zip.putNextEntry(ZipEntry(dir.name + "/" + f.relativeTo(dir).invariantSeparatorsPath))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } != null
    }.getOrDefault(false)

    private fun unzip(zipFile: File, into: File) {
        val canonicalRoot = into.canonicalPath + File.separator
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                val out = File(into, e.name)
                // reject zip-slip entries
                if (out.canonicalPath.startsWith(canonicalRoot)) {
                    if (e.isDirectory) out.mkdirs() else {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zis.copyTo(it) }
                    }
                }
                e = zis.nextEntry
            }
        }
    }
}

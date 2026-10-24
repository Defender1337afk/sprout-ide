package com.sprout.ide

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import com.sprout.ide.cloud.Badge
import com.sprout.ide.cloud.Cloud
import com.sprout.ide.cloud.Collab
import com.sprout.ide.cloud.CollabMarker
import com.sprout.ide.cloud.Collabs
import com.sprout.ide.cloud.ConflictException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.sprout.ide.cloud.UserProfile
import com.sprout.ide.data.AppSettings
import com.sprout.ide.data.ProjectInfo
import com.sprout.ide.data.ProjectTemplate
import com.sprout.ide.data.SettingsRepository
import com.sprout.ide.data.Workspace
import com.sprout.ide.editor.EditorTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface Screen {
    data object Home : Screen
    data class Project(val dir: File) : Screen
    data object Settings : Screen
    data class Preview(val file: File) : Screen
    data class Profile(val uid: String) : Screen
    data object EditProfile : Screen
    data object Community : Screen
    data class PublicProject(val id: String) : Screen
    data object Collabs : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val settingsRepo = SettingsRepository(app)
    val settings get() = settingsRepo.state.value
    val workspace = Workspace(app)
    private val sessionPrefs = app.getSharedPreferences("session", Context.MODE_PRIVATE)

    val backStack = mutableStateListOf<Screen>(Screen.Home)
    val screen: Screen get() = backStack.last()

    var projects by mutableStateOf<List<ProjectInfo>>(emptyList())
        private set
    var loadingProjects by mutableStateOf(true)
        private set

    var project by mutableStateOf<File?>(null)
        private set
    val tabs = mutableStateListOf<EditorTab>()
    var activeIndex by mutableIntStateOf(-1)
    val activeTab: EditorTab? get() = tabs.getOrNull(activeIndex)

    /** Bumped whenever the file tree on disk changes so the drawer re-reads it. */
    var treeVersion by mutableIntStateOf(0)
        private set

    private var autosaveJob: Job? = null

    // --- account --------------------------------------------------------------------------------
    private val accountPrefs = app.getSharedPreferences("account", Context.MODE_PRIVATE)
    var onboarded by mutableStateOf(accountPrefs.getBoolean("onboarded", false))
        private set
    var user by mutableStateOf<FirebaseUser?>(null)
        private set
    var me by mutableStateOf<UserProfile?>(null)
        private set
    var meLoaded by mutableStateOf(false)
        private set
    var badgeCatalog by mutableStateOf<Map<String, Badge>>(emptyMap())
        private set
    var signingIn by mutableStateOf(false)
        private set
    val isGuest: Boolean get() = user == null
    /** Signed in with Google but no profile document yet → show account creation. */
    val needsAccount: Boolean get() = user != null && meLoaded && me == null
    private var meListener: ListenerRegistration? = null

    private val authListener = FirebaseAuth.AuthStateListener { a ->
        val u = a.currentUser
        if (u?.uid == user?.uid && (u != null) == (user != null)) return@AuthStateListener
        user = u
        meListener?.remove()
        me = null
        meLoaded = false
        if (u != null) {
            meListener = Cloud.db.collection("users").document(u.uid).addSnapshotListener { snap, err ->
                if (err == null && snap != null) { me = UserProfile.from(snap); meLoaded = true }
            }
            loadBadges()
        }
    }

    init {
        refreshProjects()
        Cloud.init(app)
        if (Cloud.configured) Cloud.auth.addAuthStateListener(authListener)
    }

    fun finishOnboarding() {
        onboarded = true
        accountPrefs.edit().putBoolean("onboarded", true).apply()
    }

    fun signIn(activity: Activity, onError: (String) -> Unit) {
        if (signingIn) return
        signingIn = true
        viewModelScope.launch {
            try {
                Cloud.signInWithGoogle(activity)
                finishOnboarding()
            } catch (e: Exception) {
                onError(e.message ?: "Ошибка входа")
            } finally {
                signingIn = false
            }
        }
    }

    fun signOut() {
        Cloud.signOut()
        backStack.removeAll { it is Screen.EditProfile || it is Screen.Profile || it is Screen.Community || it is Screen.PublicProject }
        if (backStack.isEmpty()) backStack.add(Screen.Home)
    }

    /** Back to the welcome flow (used by "Войти" from guest mode). */
    fun showOnboarding() {
        onboarded = false
        accountPrefs.edit().putBoolean("onboarded", false).apply()
    }

    fun loadBadges() {
        viewModelScope.launch { runCatching { Cloud.badges() }.onSuccess { badgeCatalog = it } }
    }

    fun publishedId(dir: File): String? = accountPrefs.getString("published:" + dir.name, null)
    fun setPublishedId(dir: File, id: String?) {
        accountPrefs.edit().apply { if (id == null) remove("published:" + dir.name) else putString("published:" + dir.name, id) }.apply()
    }

    /** Copies a public project into the local workspace (the point of open source). */
    fun cloneProject(title: String, files: List<com.sprout.ide.cloud.PublicFile>, done: (File) -> Unit) {
        viewModelScope.launch {
            val dir = withContext(Dispatchers.IO) {
                val d = File(workspace.root, workspace.uniqueName(title)).apply { mkdirs() }
                val root = d.canonicalPath + File.separator
                files.forEach { f ->
                    val target = File(d, f.path)
                    if (target.canonicalPath.startsWith(root)) { target.parentFile?.mkdirs(); target.writeText(f.content) }
                }
                d
            }
            refreshProjects()
            done(dir)
        }
    }

    fun navigate(s: Screen) { backStack.add(s) }

    fun back(): Boolean {
        if (backStack.size <= 1) return false
        val top = backStack.removeAt(backStack.lastIndex)
        if (top is Screen.Project) closeProject()
        return true
    }

    fun refreshProjects() {
        viewModelScope.launch {
            loadingProjects = true
            projects = withContext(Dispatchers.IO) { workspace.projects() }
            loadingProjects = false
        }
    }

    fun createProject(name: String, template: ProjectTemplate) {
        viewModelScope.launch {
            val dir = withContext(Dispatchers.IO) { workspace.createProject(name, template) }
            refreshProjects()
            openProject(dir)
        }
    }

    fun deleteProject(p: ProjectInfo) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { workspace.delete(p.dir) }
            refreshProjects()
        }
    }

    fun renameProject(p: ProjectInfo, name: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { workspace.rename(p.dir, workspace.uniqueName(name)) }
            refreshProjects()
        }
    }

    fun openProject(dir: File) {
        project = dir
        tabs.clear()
        activeIndex = -1
        if (settings.restoreTabs) {
            val saved = sessionPrefs.getString("tabs:" + dir.name, null)?.split('\n').orEmpty()
            saved.map { File(dir, it) }.filter { it.isFile }.forEach { openFile(it, activate = false) }
            activeIndex = sessionPrefs.getInt("active:" + dir.name, 0).coerceIn(-1, tabs.lastIndex)
        }
        if (tabs.isEmpty()) {
            val main = dir.walkTopDown().filter { it.isFile }.sortedBy { if (it.name == "README.md") 1 else 0 }.firstOrNull()
            if (main != null) openFile(main)
        }
        navigate(Screen.Project(dir))
        startCollab(dir)
    }

    private fun closeProject() {
        saveAll()
        persistSession()
        collab?.stop()
        collab = null
        collabInfo = null
        conflicts.clear()
        project = null
        tabs.clear()
        activeIndex = -1
        refreshProjects()
    }

    private fun persistSession() {
        val dir = project ?: return
        sessionPrefs.edit()
            .putString("tabs:" + dir.name, tabs.joinToString("\n") { it.file.relativeTo(dir).path })
            .putInt("active:" + dir.name, activeIndex)
            .apply()
    }

    fun openFile(file: File, activate: Boolean = true) {
        val existing = tabs.indexOfFirst { it.file == file }
        if (existing >= 0) { if (activate) activeIndex = existing; return }
        if (file.length() > 4 * 1024 * 1024) return
        val text = runCatching { file.readText() }.getOrDefault("")
        tabs.add(EditorTab(file, text))
        if (activate || activeIndex < 0) activeIndex = tabs.lastIndex
        persistSession()
    }

    fun closeTab(index: Int, save: Boolean = true) {
        val tab = tabs.getOrNull(index) ?: return
        if (save && tab.dirty) save(tab)
        tabs.removeAt(index)
        activeIndex = when {
            tabs.isEmpty() -> -1
            index < activeIndex -> activeIndex - 1
            else -> activeIndex.coerceAtMost(tabs.lastIndex)
        }
        persistSession()
    }

    fun closeOthers(index: Int) {
        val keep = tabs.getOrNull(index) ?: return
        tabs.filter { it !== keep && it.dirty }.forEach { save(it) }
        tabs.retainAll { it === keep }
        activeIndex = 0
        persistSession()
    }

    fun save(tab: EditorTab) {
        val text = tab.value.text
        runCatching { tab.file.writeText(text) }.onSuccess {
            tab.savedText = text
            pushFile(tab.file, text)
        }
    }

    fun saveAll() = tabs.filter { it.dirty }.forEach { save(it) }

    fun onEdited() {
        if (!settings.autoSave) return
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(1200)
            saveAll()
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsRepo.update(transform)

    // --- file tree operations ---------------------------------------------------------------------

    fun createFile(parent: File, name: String, template: Boolean) {
        val f = workspace.createFile(parent, name, template)
        treeVersion++
        openFile(f)
        pushFile(f, runCatching { f.readText() }.getOrDefault(""))
    }

    fun createFolder(parent: File, name: String) {
        workspace.createFolder(parent, name)
        treeVersion++
    }

    fun rename(file: File, name: String) {
        val before = filesUnder(file)
        val target = workspace.rename(file, name) ?: return
        before.forEach { pushDelete(it) }
        filesUnder(target).forEach { pushFile(it, runCatching { it.readText() }.getOrDefault("")) }
        tabs.forEach { t ->
            if (t.file == file) t.file = target
            else if (t.file.path.startsWith(file.path + File.separator)) {
                t.file = File(target, t.file.relativeTo(file).path)
            }
        }
        treeVersion++
        persistSession()
    }

    fun delete(file: File) {
        tabs.indices.reversed().forEach { i ->
            val t = tabs[i]
            if (t.file == file || t.file.path.startsWith(file.path + File.separator)) closeTab(i, save = false)
        }
        filesUnder(file).forEach { pushDelete(it) }
        workspace.delete(file)
        treeVersion++
    }

    /** LICENSE may have been added on publish, so the drawer must re-read the folder. */
    fun notifyTreeChanged() { treeVersion++ }

    fun duplicate(file: File) {
        val copy = workspace.duplicate(file)
        treeVersion++
        filesUnder(copy).forEach { pushFile(it, runCatching { it.readText() }.getOrDefault("")) }
    }

    fun import(uri: Uri, into: File) {
        viewModelScope.launch {
            val f = withContext(Dispatchers.IO) { workspace.importUri(getApplication<Application>().contentResolver, uri, into) }
            treeVersion++
            if (f?.isFile == true) openFile(f)
            if (f != null) filesUnder(f).forEach { pushFile(it, runCatching { it.readText() }.getOrDefault("")) }
        }
    }

    /** A .zip becomes a project folder; any other file gets wrapped into a new "Импорт" project. */
    fun importProject(uri: Uri, done: (String?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val resolver = getApplication<Application>().contentResolver
                val tmp = File(getApplication<Application>().cacheDir, "import").apply { deleteRecursively(); mkdirs() }
                val f = workspace.importUri(resolver, uri, tmp) ?: return@withContext null
                val target = File(workspace.root, workspace.uniqueName(if (f.isDirectory) f.name else "Импорт"))
                if (f.isDirectory) {
                    // unwrap a single top-level folder inside the archive
                    val only = f.listFiles()?.singleOrNull()?.takeIf { it.isDirectory } ?: f
                    only.copyRecursively(target)
                } else {
                    target.mkdirs(); f.copyTo(File(target, f.name))
                }
                tmp.deleteRecursively()
                target.name
            }
            refreshProjects()
            done(result)
        }
    }

    fun exportZip(dir: File, uri: Uri, done: (Boolean) -> Unit) {
        saveAll()
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { workspace.exportZip(getApplication<Application>().contentResolver, dir, uri) }
            done(ok)
        }
    }

    // --- shared projects ----------------------------------------------------------------------------
    var collab by mutableStateOf<Collabs.Session?>(null)
        private set
    var collabInfo by mutableStateOf<Collab?>(null)
        private set
    /** Files changed by someone else while this user had unsaved edits in them. */
    val conflicts = mutableStateListOf<ConflictException>()
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    private fun filesUnder(f: File): List<File> =
        if (f.isDirectory) f.walkTopDown().filter { it.isFile && it.name != CollabMarker.FILE }.toList()
        else if (f.isFile) listOf(f) else emptyList()

    private fun relPath(f: File): String? {
        val dir = project ?: return null
        val rel = runCatching { f.relativeTo(dir).invariantSeparatorsPath }.getOrNull() ?: return null
        return rel.takeIf { !it.startsWith("..") && it != CollabMarker.FILE }
    }

    private fun startCollab(dir: File) {
        val marker = CollabMarker.read(dir) ?: return
        val profile = me
        if (profile == null) {
            _events.tryEmit("Совместный проект: войдите в аккаунт, чтобы синхронизировать изменения")
            return
        }
        val session = Collabs.Session(marker, profile) { path, content, by, version -> applyRemote(dir, path, content, by, version) }
        collab = session
        session.start()
        viewModelScope.launch { collabInfo = runCatching { Collabs.get(marker.id) }.getOrNull() }
    }

    fun refreshCollabInfo() {
        val id = collab?.id ?: return
        viewModelScope.launch { collabInfo = runCatching { Collabs.get(id) }.getOrNull() }
    }

    private fun applyRemote(dir: File, path: String, content: String?, by: String, version: Long): Boolean {
        val file = File(dir, path)
        if (!file.canonicalPath.startsWith(dir.canonicalPath + File.separator)) return true
        val tab = tabs.firstOrNull { it.file == file }
        if (tab != null && tab.dirty) {
            if (conflicts.none { it.path == path }) conflicts.add(ConflictException(path, content ?: "", by, version))
            _events.tryEmit("@$by изменил $path, пока вы его редактировали")
            return false
        }
        if (content == null) {
            tabs.indexOf(tab).takeIf { it >= 0 }?.let { closeTab(it, save = false) }
            file.delete()
            treeVersion++
            if (by.isNotEmpty()) _events.tryEmit("@$by удалил $path")
            return true
        }
        val isNew = !file.exists()
        file.parentFile?.mkdirs()
        file.writeText(content)
        tab?.replaceExternal(content)
        if (isNew) treeVersion++
        if (by.isNotEmpty() && by != me?.username) _events.tryEmit("@$by обновил $path")
        return true
    }

    private fun pushFile(file: File, text: String) {
        val session = collab ?: return
        val path = relPath(file) ?: return
        viewModelScope.launch {
            try {
                session.push(path, text)
            } catch (e: ConflictException) {
                if (conflicts.none { it.path == path }) conflicts.add(e)
                _events.tryEmit("Конфликт: @${e.by} уже изменил $path")
            } catch (e: Exception) {
                _events.tryEmit("Не удалось синхронизировать $path: ${e.message}")
            }
        }
    }

    private fun pushDelete(file: File) {
        val session = collab ?: return
        val path = relPath(file) ?: return
        viewModelScope.launch { runCatching { session.pushDelete(path) } }
    }

    /** keepMine = overwrite the shared copy with the local text; otherwise take the other person's version. */
    fun resolveConflict(c: ConflictException, keepMine: Boolean) {
        val session = collab ?: return
        val dir = project ?: return
        val file = File(dir, c.path)
        val tab = tabs.firstOrNull { it.file == file }
        conflicts.remove(c)
        viewModelScope.launch {
            try {
                if (keepMine) {
                    val text = tab?.value?.text ?: file.readText()
                    file.writeText(text)
                    tab?.savedText = text
                    session.push(c.path, text, force = true)
                } else {
                    file.parentFile?.mkdirs()
                    file.writeText(c.remoteContent)
                    tab?.replaceExternal(c.remoteContent)
                    session.acceptRemote(c.path, c.remoteVersion)
                    treeVersion++
                }
            } catch (e: Exception) {
                _events.tryEmit("Ошибка: ${e.message}")
            }
        }
    }

    /** Turns a local project into a shared one. */
    fun makeShared(dir: File, done: (String?) -> Unit) {
        val profile = me ?: return done("Нужен аккаунт")
        viewModelScope.launch {
            saveAll()
            runCatching { Collabs.create(profile, dir) }
                .onSuccess { c ->
                    if (project == dir) startCollab(dir)
                    collabInfo = c
                    refreshProjects()
                    done(null)
                }
                .onFailure { done(it.message ?: "Ошибка") }
        }
    }

    /** Opens the local copy of a shared project, downloading it first if needed (optionally accepting the invite). */
    fun openCollab(c: Collab, accept: Boolean, done: (String?) -> Unit) {
        val profile = me ?: return done("Нужен аккаунт")
        viewModelScope.launch {
            try {
                if (accept) Collabs.accept(c, profile)
                val local = withContext(Dispatchers.IO) {
                    workspace.root.listFiles().orEmpty().firstOrNull { it.isDirectory && CollabMarker.read(it)?.id == c.id }
                } ?: File(workspace.root, workspace.uniqueName(c.title)).also { Collabs.download(c, it) }
                refreshProjects()
                openProject(local)
                done(null)
            } catch (e: Exception) {
                done(e.message ?: "Ошибка")
            }
        }
    }

    /** After leaving or deleting a shared project the local files stay as an ordinary project. */
    fun detachCollab(dir: File) {
        File(dir, CollabMarker.FILE).delete()
        if (project == dir) {
            collab?.stop()
            collab = null
            collabInfo = null
            conflicts.clear()
        }
        refreshProjects()
    }

    override fun onCleared() {
        collab?.stop()
        if (Cloud.configured) Cloud.auth.removeAuthStateListener(authListener)
        meListener?.remove()
        saveAll()
        persistSession()
    }
}

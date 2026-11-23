package com.notes.satu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesViewModel : ViewModel() {
    // database di-inject lewat init dari activity
    lateinit var dao: NoteDao
    val notes = kotlinx.coroutines.flow.MutableStateFlow<List<Note>>(emptyList())

    fun load() {
        viewModelScope.launch {
            dao.getAll().collect { notes.value = it }
        }
    }
    fun add(title: String, content: String) = viewModelScope.launch {
        dao.insert(Note(title = title, content = content))
    }
    fun toggleLock(note: Note) = viewModelScope.launch {
        dao.update(note.copy(isLocked = !note.isLocked))
    }
}

class MainActivity : ComponentActivity() {
    private val vm by viewModels<NotesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.dao = AppDatabase.get(this).noteDao()
        vm.load()
        setContent { NotesScreen(vm) }
    }
}

val NotesYellow = Color(0xFFF7E9A0)
val NotesBg = Color(0xFFFFFDF5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(vm: NotesViewModel) {
    val notes by vm.notes.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var unlockedIds by remember { mutableStateOf(setOf<Long>()) }

    MaterialTheme(colorScheme = lightColorScheme(background = NotesBg)) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    containerColor = NotesYellow,
                    onClick = { showEditor = true }
                ) { Text("+", fontSize = 24.sp) }
            }
        ) { padding ->
            Column(Modifier.padding(padding).background(NotesBg).fillMaxSize()) {
                Text(
                    "1Notes",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                LazyColumn {
                    items(notes) { note ->
                        val locked = note.isLocked && note.id !in unlockedIds
                        NoteRow(note, locked, onClick = {
                            if (note.isLocked && note.id !in unlockedIds) {
                                // activity context diperlukan; sederhanakan: minta PIN
                                (vm as? NotesViewModel) // biometrik dipanggil dari activity
                            } else {
                                showEditor = true
                            }
                        }, onLockToggle = { vm.toggleLock(note) })
                    }
                }
            }
        }
    }
    if (showEditor) {
        EditorDialog(onSave = { title, content ->
            vm.add(title, content)
            showEditor = false
        }, onDismiss = { showEditor = false })
    }
}

@Composable
fun NoteRow(note: Note, locked: Boolean, onClick: () -> Unit, onLockToggle: () -> Unit) {
    val dateFmt = SimpleDateFormat("d MMM yyyy", Locale("id"))
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (locked) "Catatan Terkunci" else note.title.ifEmpty { "Catatan Baru" },
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                "${dateFmt.format(Date(note.modifiedAt))} · " +
                    (if (locked) "•••" else note.content.take(40)),
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
        Text(
            if (note.isLocked) "🔒" else "🔓",
            Modifier.clickable(onClick = onLockToggle).padding(8.dp)
        )
    }
}

@Composable
fun EditorDialog(onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, color = NotesBg) {
            Column(Modifier.padding(16.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Judul") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Mulai menulis...") },
                    minLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Button(
                        onClick = { onSave(title, content) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6C700))
                    ) { Text("Simpan") }
                }
            }
        }
    }
}

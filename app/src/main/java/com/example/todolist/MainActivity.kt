package com.example.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.todolist.ui.theme.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject
import org.json.JSONArray

data class Task(
    val id: String,
    val text: String,
    val date: String,
    val time: String,
    var isCompleted: Boolean = false
)

class MainActivity : ComponentActivity() {
    private val FILENAME = "todo_data.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ToDoListTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ToDoListApp(
                        loadTasks = { loadTasksFromFile() },
                        saveTasks = { tasks -> saveTasksToFile(tasks) }
                    )
                }
            }
        }
    }

    private fun loadTasksFromFile(): MutableList<Task> {
        return try {
            val file = File(filesDir, FILENAME)
            if (!file.exists()) return mutableListOf()

            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val tasks = mutableListOf<Task>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                tasks.add(
                    Task(
                        id = jsonObject.getString("id"),
                        text = jsonObject.getString("text"),
                        date = jsonObject.getString("date"),
                        time = jsonObject.getString("time"),
                        isCompleted = jsonObject.getBoolean("isCompleted")
                    )
                )
            }
            tasks
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveTasksToFile(tasks: List<Task>) {
        try {
            val jsonArray = JSONArray()
            tasks.forEach { task ->
                val jsonObject = JSONObject().apply {
                    put("id", task.id)
                    put("text", task.text)
                    put("date", task.date)
                    put("time", task.time)
                    put("isCompleted", task.isCompleted)
                }
                jsonArray.put(jsonObject)
            }

            val file = File(filesDir, FILENAME)
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToDoListApp(
    loadTasks: () -> MutableList<Task>,
    saveTasks: (List<Task>) -> Unit
) {
    val currentDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val currentTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
    var taskText by remember { mutableStateOf("") }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    val tasks = remember { mutableStateListOf<Task>().apply { addAll(loadTasks()) }}

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with Date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Tasks",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = currentDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = taskText,
                onValueChange = { taskText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("What needs to be done?") },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = {
                    if (taskText.isNotBlank()) {
                        if (editingTask != null) {
                            // Update existing task
                            val index = tasks.indexOfFirst { it.id == editingTask?.id }
                            if (index != -1) {
                                tasks[index] = tasks[index].copy(text = taskText)
                            }
                            editingTask = null
                        } else {
                            // Add new task
                            tasks.add(
                                Task(
                                    id = UUID.randomUUID().toString(),
                                    text = taskText,
                                    date = currentDate,
                                    time = currentTime
                                )
                            )
                        }
                        taskText = ""
                        saveTasks(tasks)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                content = {
                    Icon(
                        imageVector = if (editingTask != null) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = if (editingTask != null) "Update" else "Add"
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tasks List
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = tasks.sortedBy { it.date + it.time },
                key = { it.id }
            ) { task ->
                TaskItem(
                    task = task,
                    onComplete = { completed ->
                        val index = tasks.indexOfFirst { it.id == task.id }
                        if (index != -1) {
                            tasks[index] = tasks[index].copy(isCompleted = completed)
                            saveTasks(tasks)
                        }
                    },
                    onEdit = {
                        editingTask = task
                        taskText = task.text
                    },
                    onDelete = {
                        tasks.remove(task)
                        saveTasks(tasks)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: Task,
    onComplete: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onComplete,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.text,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${task.date} • ${task.time}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentMemory
import com.example.ui.theme.CampusOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorySheet(
    memory: StudentMemory,
    onUpdateName: (String) -> Unit,
    onUpdateMajor: (String) -> Unit,
    onUpdateYear: (String) -> Unit,
    onUpdateUniversity: (String) -> Unit,
    onAddCustomFact: (String, String) -> Unit,
    onClearMemory: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var editMode by remember { mutableStateOf(false) }

    var tempName by remember(memory.name) { mutableStateOf(memory.name.orEmpty()) }
    var tempMajor by remember(memory.major) { mutableStateOf(memory.major.orEmpty()) }
    var tempYear by remember(memory.year) { mutableStateOf(memory.year.orEmpty()) }
    var tempUni by remember(memory.university) { mutableStateOf(memory.university.orEmpty()) }

    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Memory",
                        tint = CampusOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Student Memory Layer",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Auto-detected from your messages and remembered",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = { editMode = !editMode },
                    modifier = Modifier.testTag("toggle_edit_memory_button")
                ) {
                    Text(if (editMode) "Done" else "Edit")
                }
            }

            if (editMode) {
                // Editable form
                OutlinedTextField(
                    value = tempName,
                    onValueChange = {
                        tempName = it
                        onUpdateName(it)
                    },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Aazan Khan") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("input_memory_name")
                )

                OutlinedTextField(
                    value = tempMajor,
                    onValueChange = {
                        tempMajor = it
                        onUpdateMajor(it)
                    },
                    label = { Text("Major / Field") },
                    placeholder = { Text("e.g. Computer Science") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("input_memory_major")
                )

                OutlinedTextField(
                    value = tempYear,
                    onValueChange = {
                        tempYear = it
                        onUpdateYear(it)
                    },
                    label = { Text("Academic Year") },
                    placeholder = { Text("e.g. Sophomore or 2nd year") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("input_memory_year")
                )

                OutlinedTextField(
                    value = tempUni,
                    onValueChange = {
                        tempUni = it
                        onUpdateUniversity(it)
                    },
                    label = { Text("University / Institution") },
                    placeholder = { Text("e.g. IIUI University") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("input_memory_university")
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Add Custom Fact (e.g. Target GPA, Research Topic):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        placeholder = { Text("Key (e.g. Interests)") },
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    )
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        placeholder = { Text("Value (e.g. Generative AI)") },
                        modifier = Modifier.weight(1.2f).padding(end = 4.dp)
                    )
                    IconButton(
                        onClick = {
                            if (newKey.isNotBlank() && newValue.isNotBlank()) {
                                onAddCustomFact(newKey, newValue)
                                newKey = ""
                                newValue = ""
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add fact", tint = CampusOrange)
                    }
                }
            } else {
                // Read-only memory card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (memory.isEmpty) {
                            Text(
                                text = "💡 Nothing remembered yet.\n\nType in chat like \"My name is Alice and I'm studying CS at MIT\" or tap 'Edit' above to save your details!",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        } else {
                            MemoryFactRow("👤 Name", memory.name ?: "Not specified")
                            MemoryFactRow("📚 Major", memory.major ?: "Not specified")
                            MemoryFactRow("🎓 Year", memory.year?.replaceFirstChar { it.uppercase() } ?: "Not specified")
                            MemoryFactRow("🏛️ University", memory.university ?: "Not specified")
                            for ((k, v) in memory.customFacts) {
                                MemoryFactRow("📌 ${k.replaceFirstChar { it.uppercase() }}", v)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (!memory.isEmpty) {
                    Button(
                        onClick = onClearMemory,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.testTag("clear_memory_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Memory")
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryFactRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (value != "Not specified") CampusOrange else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

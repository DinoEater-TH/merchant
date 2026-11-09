package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberUser
import com.example.data.model.Order
import com.example.ui.MainViewModel
import com.example.ui.theme.OrangePrimary

@Composable
fun MembersView(viewModel: MainViewModel) {
    val users by viewModel.users.collectAsState()
    val orders by viewModel.orders.collectAsState()
    var selectedPhone by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editPhone by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Add member button
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = { showAddDialog = true }) {
                androidx.compose.material3.Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("เพิ่มสมาชิก", fontSize = 12.sp)
            }
        }

        if (users.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ยังไม่มีสมาชิก", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users.entries.toList(), key = { it.key }) { (phone, u) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = phone, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = if (u.name.isBlank()) "-" else u.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "Stamp: ${u.stamps} ดวง • ออเดอร์: ${u.totalOrders}", fontSize = 12.sp, color = OrangePrimary)
                            }
                            TextButton(onClick = { selectedPhone = phone }) {
                                Text("ดู", fontSize = 11.sp, color = OrangePrimary)
                            }
                            TextButton(onClick = { editPhone = phone }) {
                                Text("แก้ไข", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add member dialog
    if (showAddDialog) {
        var newPhone by remember { mutableStateOf("") }
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("เพิ่มสมาชิกใหม่", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("เบอร์โทร (10 หลัก)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("ชื่อ") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPhone.trim().length == 10) {
                        viewModel.addOrUpdateMember(newPhone.trim(), newName.trim())
                        showAddDialog = false
                    }
                }) { Text("บันทึก") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("ยกเลิก") }
            }
        )
    }

    // Edit member dialog
    editPhone?.let { phone ->
        val u = users[phone] ?: MemberUser(phoneNumber = phone)
        var editName by remember { mutableStateOf(u.name) }
        var editPhoneVal by remember { mutableStateOf(phone) }
        AlertDialog(
            onDismissRequest = { editPhone = null },
            title = { Text("แก้ไขสมาชิก", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = editPhoneVal, onValueChange = { editPhoneVal = it }, label = { Text("เบอร์โทร (10 หลัก)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("ชื่อ") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editPhoneVal.trim().length == 10) {
                        if (editPhoneVal.trim() != phone) {
                            viewModel.deleteMember(phone)
                        }
                        viewModel.addOrUpdateMember(editPhoneVal.trim(), editName.trim())
                        editPhone = null
                    }
                }) { Text("บันทึก") }
            },
            dismissButton = {
                TextButton(onClick = { editPhone = null }) { Text("ยกเลิก") }
            }
        )
    }

    // Detail dialog
    selectedPhone?.let { phone ->
        val u = users[phone] ?: MemberUser(phoneNumber = phone)
        val memberOrders = orders.filter { it.customerPhone == phone }
        AlertDialog(
            onDismissRequest = { selectedPhone = null },
            title = { Text("สมาชิก $phone", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("ชื่อ: ${if (u.name.isBlank()) "-" else u.name}", fontSize = 13.sp)
                    Text("เบอร์โทร: $phone", fontSize = 13.sp)
                    Text("Stamp สะสม: ${u.stamps} ดวง", fontSize = 13.sp, color = OrangePrimary, fontWeight = FontWeight.Bold)
                    Text("จำนวนออเดอร์: ${u.totalOrders}", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ประวัติการสั่งซื้อ (${memberOrders.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (memberOrders.isEmpty()) {
                        Text("ยังไม่มีประวัติ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        memberOrders.forEach { o ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("${o.id} • ${o.timestamp}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text((o.items ?: emptyList()).joinToString(", ") { "${it.name} x${it.quantity}" }, fontSize = 11.sp)
                                Text("฿${o.totalAmount}", fontSize = 11.sp, color = OrangePrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPhone = null }) { Text("ปิด") }
            }
        )
    }
}
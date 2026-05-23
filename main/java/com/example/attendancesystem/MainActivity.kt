package com.example.attendancesystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth

val IndigoPrimary = Color(0xFF3F51B5)
val LightBackground = Color(0xFFF5F7FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = IndigoPrimary,
                    background = LightBackground,
                    surface = Color.White
                )
            ) {
                val authViewModel: AuthViewModel = viewModel()
                val loginStatus by authViewModel.loginStatus.collectAsState()
                var screenState by remember { mutableStateOf("login") }

                // Persistence Logic: Decide screen based on login status
                LaunchedEffect(loginStatus) {
                    if (loginStatus.contains("Success", ignoreCase = true)) {
                        screenState = "dashboard"
                    } else if (loginStatus.isEmpty()) {
                        screenState = "login"
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(LightBackground)) {
                        when (screenState) {
                            "login" -> LoginScreen(authViewModel = authViewModel)
                            "dashboard" -> DashboardScreen(
                                authViewModel = authViewModel,
                                onNavigateToAttendance = { screenState = "attendance" },
                                onNavigateToStudentView = { screenState = "student_view" },
                                onNavigateToReports = { screenState = "reports" }
                            )
                            "attendance" -> AttendanceScreen(authViewModel = authViewModel, onBack = { screenState = "dashboard" })
                            "student_view" -> StudentViewScreen(authViewModel = authViewModel, onBack = { screenState = "dashboard" })
                            "reports" -> ReportsScreen(authViewModel = authViewModel, onBack = { screenState = "dashboard" })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Student") }
    val loginStatus by authViewModel.loginStatus.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.School, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(80.dp))
        Text("Smart Attend", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
        Text("Your College Companion", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(48.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("College Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().selectableGroup(), Arrangement.SpaceEvenly) {
            FilterChip(selected = selectedRole == "Student", onClick = { selectedRole = "Student" }, label = { Text("Student") })
            FilterChip(selected = selectedRole == "Teacher", onClick = { selectedRole = "Teacher" }, label = { Text("Teacher") })
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = { authViewModel.loginUser(email, password) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) { Text("LOGIN", fontWeight = FontWeight.Bold) }
        TextButton(onClick = { authViewModel.registerUser(email, password, selectedRole) }) { Text("Create New Account", color = IndigoPrimary) }
        if (loginStatus.isNotEmpty() && !loginStatus.contains("Success")) Text(loginStatus, color = IndigoPrimary, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
fun DashboardScreen(authViewModel: AuthViewModel, onNavigateToAttendance: () -> Unit, onNavigateToStudentView: () -> Unit, onNavigateToReports: () -> Unit) {
    val role by authViewModel.userRole.collectAsState()
    val email = FirebaseAuth.getInstance().currentUser?.email

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Welcome back,", fontSize = 16.sp, color = Color.Gray)
        Text(email?.split("@")?.get(0) ?: "User", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = IndigoPrimary)) {
            Column(Modifier.padding(24.dp)) {
                Text("Role: ${role ?: "..."}", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Bharati Vidyapeeth College of Engineering Pune", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
        if (role == "Teacher") {
            DashboardButton("Take Attendance", Icons.Default.CheckCircle, onNavigateToAttendance)
            DashboardButton("Class Reports", Icons.Default.Analytics, onNavigateToReports)
        } else if (role == "Student") {
            DashboardButton("My Attendance %", Icons.Default.Person, onNavigateToStudentView)
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = { authViewModel.logout() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Logout", color = Color.Red)
        }
    }
}

@Composable
fun DashboardButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(64.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = IndigoPrimary), elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(16.dp))
            Text(text, fontWeight = FontWeight.Medium, fontSize = 18.sp)
        }
    }
}

@Composable
fun AttendanceScreen(authViewModel: AuthViewModel, onBack: () -> Unit) {
    val students by authViewModel.studentsList.collectAsState()
    val attendanceMap = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(Unit) { authViewModel.fetchStudents() }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            Text("Mark Attendance", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(students) { student ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(student.email, fontWeight = FontWeight.Medium)
                        val isChecked = attendanceMap[student.id] ?: false
                        Switch(checked = isChecked, onCheckedChange = { attendanceMap[student.id] = it })
                    }
                }
            }
        }
        Button(onClick = {
            val finalizedList = students.map { it.copy(isPresent = attendanceMap[it.id] ?: false) }
            authViewModel.submitAttendance(finalizedList)
            onBack()
        }, Modifier.fillMaxWidth().height(56.dp).padding(top = 16.dp), shape = RoundedCornerShape(12.dp)) { Text("SUBMIT") }
    }
}

@Composable
fun StudentViewScreen(authViewModel: AuthViewModel, onBack: () -> Unit) {
    val presentCount by authViewModel.attendanceCount.collectAsState()
    val totalDays by authViewModel.totalDays.collectAsState()
    val percentage = if (totalDays > 0) (presentCount.toFloat() / totalDays * 100).toInt() else 0

    LaunchedEffect(Unit) { authViewModel.fetchMyAttendance() }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Your Attendance", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(48.dp))
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { percentage / 100f }, modifier = Modifier.size(200.dp), strokeWidth = 12.dp, color = if (percentage >= 75) IndigoPrimary else Color.Red)
            Text("$percentage%", fontSize = 48.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(48.dp))
        Text("Classes Attended: $presentCount / $totalDays")
        Button(onClick = onBack, modifier = Modifier.padding(top = 48.dp)) { Text("Back to Dashboard") }
    }
}

@Composable
fun ReportsScreen(authViewModel: AuthViewModel, onBack: () -> Unit) {
    val history by authViewModel.attendanceHistory.collectAsState()
    val defaulters by authViewModel.defaulterList.collectAsState()
    LaunchedEffect(Unit) { authViewModel.fetchAttendanceHistory(); authViewModel.fetchDefaulters() }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            Text("Class Reports", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text("Attendance History", fontWeight = FontWeight.Bold, color = Color.Gray)
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(history) { record ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(16.dp), Arrangement.SpaceBetween) {
                        Text(record.date, fontWeight = FontWeight.Bold)
                        Text("${record.presentCount} Present", color = IndigoPrimary)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Defaulters (<75%)", fontWeight = FontWeight.Bold, color = Color.Red)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(defaulters) { student ->
                Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween) {
                    Text(student.email)
                    Text("${student.attendancePercentage}%", fontWeight = FontWeight.Bold, color = Color.Red)
                }
            }
        }
    }
}
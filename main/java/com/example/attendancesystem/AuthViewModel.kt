package com.example.attendancesystem

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

data class Student(
    val id: String = "",
    val email: String = "",
    var isPresent: Boolean = false,
    var attendancePercentage: Int = 0
)

data class AttendanceRecord(
    val id: String = "",
    val date: String = "",
    val presentCount: Int = 0
)

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _loginStatus = MutableStateFlow<String>("")
    val loginStatus: StateFlow<String> = _loginStatus

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    private val _studentsList = MutableStateFlow<List<Student>>(emptyList())
    val studentsList: StateFlow<List<Student>> = _studentsList

    private val _attendanceCount = MutableStateFlow(0)
    val attendanceCount: StateFlow<Int> = _attendanceCount

    private val _totalDays = MutableStateFlow(0)
    val totalDays: StateFlow<Int> = _totalDays

    private val _attendanceHistory = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceHistory: StateFlow<List<AttendanceRecord>> = _attendanceHistory

    private val _defaulterList = MutableStateFlow<List<Student>>(emptyList())
    val defaulterList: StateFlow<List<Student>> = _defaulterList

    // --- NEW: Check for existing session on startup ---
    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _loginStatus.value = "Success"
            fetchUserRole()
        }
    }

    fun loginUser(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _loginStatus.value = "Please enter email and password"
            return
        }
        _loginStatus.value = "Logging in..."
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _loginStatus.value = "Success"
                    fetchUserRole()
                } else {
                    _loginStatus.value = "Error: ${task.exception?.message}"
                }
            }
    }

    fun registerUser(email: String, password: String, role: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _loginStatus.value = "Please enter email and password"
            return
        }
        _loginStatus.value = "Creating account..."
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    val userProfile = hashMapOf("email" to email, "role" to role)
                    if (userId != null) {
                        db.collection("Users").document(userId).set(userProfile)
                            .addOnSuccessListener {
                                _loginStatus.value = "Success"
                                _userRole.value = role
                            }
                    }
                } else {
                    _loginStatus.value = "Error: ${task.exception?.message}"
                }
            }
    }

    fun fetchUserRole() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("Users").document(userId).get()
                .addOnSuccessListener { _userRole.value = it.getString("role") }
        }
    }

    fun fetchStudents() {
        db.collection("Users")
            .whereEqualTo("role", "Student")
            .get()
            .addOnSuccessListener { result ->
                val students = result.map { doc ->
                    Student(id = doc.id, email = doc.getString("email") ?: "")
                }
                _studentsList.value = students
            }
    }

    fun submitAttendance(students: List<Student>) {
        val teacherId = auth.currentUser?.uid
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val presentStudentIds = students.filter { it.isPresent }.map { it.id }

        val record = hashMapOf(
            "date" to date,
            "teacherId" to teacherId,
            "presentStudents" to presentStudentIds
        )

        db.collection("AttendanceRecords").add(record)
            .addOnSuccessListener { _loginStatus.value = "Attendance Submitted Successfully!" }
    }

    fun fetchMyAttendance() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("AttendanceRecords").get().addOnSuccessListener { result ->
            _totalDays.value = result.size()
            var count = 0
            for (doc in result) {
                val presentList = doc.get("presentStudents") as? List<*>
                if (presentList?.contains(userId) == true) count++
            }
            _attendanceCount.value = count
        }
    }

    fun fetchAttendanceHistory() {
        db.collection("AttendanceRecords")
            .orderBy("date")
            .get()
            .addOnSuccessListener { result ->
                val history = result.map { doc ->
                    val presentList = doc.get("presentStudents") as? List<*>
                    AttendanceRecord(
                        id = doc.id,
                        date = doc.getString("date") ?: "",
                        presentCount = presentList?.size ?: 0
                    )
                }
                _attendanceHistory.value = history
            }
    }

    fun fetchDefaulters() {
        db.collection("AttendanceRecords").get().addOnSuccessListener { records ->
            val totalClasses = records.size()
            if (totalClasses == 0) return@addOnSuccessListener
            db.collection("Users").whereEqualTo("role", "Student").get().addOnSuccessListener { studentDocs ->
                val defaulters = mutableListOf<Student>()
                for (doc in studentDocs) {
                    var attendedCount = 0
                    for (record in records) {
                        val presentList = record.get("presentStudents") as? List<*>
                        if (presentList?.contains(doc.id) == true) attendedCount++
                    }
                    val percent = (attendedCount.toFloat() / totalClasses * 100).toInt()
                    if (percent < 75) {
                        defaulters.add(Student(id = doc.id, email = doc.getString("email") ?: "", attendancePercentage = percent))
                    }
                }
                _defaulterList.value = defaulters
            }
        }
    }

    fun logout() {
        auth.signOut()
        _loginStatus.value = ""
        _userRole.value = null
    }
}
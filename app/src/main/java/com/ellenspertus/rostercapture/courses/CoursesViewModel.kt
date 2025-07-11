package com.ellenspertus.rostercapture.courses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ellenspertus.rostercapture.instrumentation.Analytics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CoursesViewModel(application: Application) : AndroidViewModel(application) {

    private val coursesRepository = CoursesRepository(application)

    val courses: StateFlow<List<Course>> = coursesRepository.coursesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCourse(course: Course) {
        Analytics.logFirstNTimes(10, "course_created")
        viewModelScope.launch {
            coursesRepository.addCourse(course)
        }
    }

    fun removeCourse(crn: String) {
        Analytics.logFirstNTimes(10, "course_removed")
        viewModelScope.launch {
            coursesRepository.removeCourse(crn)
        }
    }
}
package com.ellenspertus.qroster.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ellenspertus.qroster.data.CoursesRepository
import com.ellenspertus.qroster.model.Course
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CoursesViewModel(application: Application) : AndroidViewModel(application) {

    private val coursesRepository = CoursesRepository(application)

    // StateFlow that emits the current list of courses
    val courses: StateFlow<List<Course>> = coursesRepository.coursesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCourse(course: Course) {
        viewModelScope.launch {
            coursesRepository.addCourse(course)
        }
    }

    fun removeCourse(crn: String) {
        viewModelScope.launch {
            coursesRepository.removeCourse(crn)
        }
    }

    fun clearAllCourses() {
        viewModelScope.launch {
            coursesRepository.clearAllCourses()
        }
    }
}
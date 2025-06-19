package com.ellenspertus.qroster

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.ellenspertus.qroster.proto.CourseProto
import com.ellenspertus.qroster.proto.CoursesProto
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream

private val Context.coursesDataStore: DataStore<CoursesProto> by dataStore(
    fileName = "courses.pb",
    serializer = CoursesSerializer
)

class CoursesRepository(
    private val context: Context
) {
    class DuplicateCourseException() :
        Exception("A course with that CRN already exists")

    val coursesFlow: Flow<List<Course>> = context.coursesDataStore.data
        .map { coursesProto ->
            coursesProto.coursesList.map { courseProto ->
                Course(
                    crn = courseProto.crn,
                    id = courseProto.id,
                    name = courseProto.name
                )
            }
        }

    suspend fun addCourse(course: Course) {
        context.coursesDataStore.updateData { currentCourses ->
            val existingCourse = currentCourses.coursesList.find { it.crn == course.crn }
            if (existingCourse != null) {
                throw DuplicateCourseException()
            } else {
                currentCourses.toBuilder()
                    .addCourses(
                        CourseProto.newBuilder()
                            .setCrn(course.crn)
                            .setId(course.id)
                            .setName(course.name)
                            .build()
                    )
                    .build()
            }
        }
    }

    suspend fun removeCourse(crn: String) {
        context.coursesDataStore.updateData { currentCourses ->
            val updatedList = currentCourses.coursesList.filter { it.crn != crn }
            currentCourses.toBuilder()
                .clearCourses()
                .addAllCourses(updatedList)
                .build()
        }
    }

    suspend fun clearAllCourses() {
        context.coursesDataStore.updateData {
            it.toBuilder().clearCourses().build()
        }
    }

    suspend fun getAllCourses(): List<Course> {
        return coursesFlow.first()
    }
}

object CoursesSerializer : Serializer<CoursesProto> {
    override val defaultValue: CoursesProto = CoursesProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): CoursesProto {
        try {
            return CoursesProto.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: CoursesProto, output: OutputStream) {
        t.writeTo(output)
    }
}

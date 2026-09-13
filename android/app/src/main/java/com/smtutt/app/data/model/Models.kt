package com.smtutt.app.data.model

import com.google.gson.annotations.SerializedName

data class GroupInfo(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String
)

data class CourseInfo(
    @SerializedName("course") val course: String,
    @SerializedName("groups") val groups: List<GroupInfo>
)

data class FacultyInfo(
    @SerializedName("faculty") val faculty: String,
    @SerializedName("courses") val courses: List<CourseInfo>
)

data class TeacherSearchResult(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String
)

data class Lesson(
    @SerializedName("time") val time: String,
    @SerializedName("week_type") val weekType: String,
    @SerializedName("dates") val dates: String,
    @SerializedName("classroom") val classroom: String,
    @SerializedName("group") val group: String = "",
    @SerializedName("subject") val subject: String,
    @SerializedName("lesson_type") val lessonType: String? = null,
    @SerializedName("subgroup") val subgroup: String? = null,
    @SerializedName("teacher_name") val teacherName: String = "",
    @SerializedName("teacher_id") val teacherId: String = ""
)

data class DaySchedule(
    @SerializedName("day_name") val dayName: String,
    @SerializedName("lessons") val lessons: List<Lesson>
)

data class ScheduleResponse(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("is_teacher") val isTeacher: Boolean,
    @SerializedName("days") val days: List<DaySchedule>,
    @SerializedName("cached_at") val cachedAt: String? = null
)

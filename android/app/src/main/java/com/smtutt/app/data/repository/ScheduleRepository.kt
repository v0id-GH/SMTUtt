package com.smtutt.app.data.repository

import com.google.gson.Gson
import com.smtutt.app.data.local.CachedScheduleEntity
import com.smtutt.app.data.local.FavoriteEntity
import com.smtutt.app.data.local.ScheduleDao
import com.smtutt.app.data.model.FacultyInfo
import com.smtutt.app.data.model.GroupInfo
import com.smtutt.app.data.model.ScheduleResponse
import com.smtutt.app.data.model.TeacherSearchResult
import com.smtutt.app.data.parser.SmtuWebParser
import com.smtutt.app.data.remote.SmtuClient
import com.smtutt.app.ui.viewmodel.WeekFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

sealed class Resource<out T> {
    data class Success<out T>(val data: T, val isOffline: Boolean = false) : Resource<T>()
    data class Error(val message: String, val cachedData: Any? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}

class ScheduleRepository(
    private val client: SmtuClient,
    private val dao: ScheduleDao,
    private val gson: Gson = Gson()
) {
    // In-memory cache for faculties list
    private var cachedFaculties: List<FacultyInfo>? = null

    suspend fun getFaculties(forceRefresh: Boolean = false): List<FacultyInfo> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedFaculties != null) {
            return@withContext cachedFaculties!!
        }

        val html = client.fetchListScheduleHtml()
        val parsed = SmtuWebParser.parseFacultiesAndGroups(html)
        cachedFaculties = parsed
        parsed
    }

    suspend fun detectAndSyncWeek(): WeekFilter? = withContext(Dispatchers.IO) {
        try {
            val html = client.fetchListScheduleHtml()
            SmtuWebParser.parseCurrentWeekFromListSchedule(html)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getGroups(search: String? = null, faculty: String? = null): List<GroupInfo> = withContext(Dispatchers.IO) {
        val faculties = getFaculties()
        val results = mutableListOf<GroupInfo>()
        val seen = mutableSetOf<String>()
        val searchClean = search?.trim()?.lowercase()

        for (f in faculties) {
            if (faculty != null && !f.faculty.contains(faculty, ignoreCase = true)) {
                continue
            }
            for (c in f.courses) {
                for (g in c.groups) {
                    if (seen.add(g.id)) {
                        if (searchClean != null && !g.name.lowercase().contains(searchClean)) {
                            continue
                        }
                        results.add(g)
                    }
                }
            }
        }
        results
    }

    suspend fun searchTeachers(query: String): List<TeacherSearchResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()

        // 1. Fetch listschedule to establish session & get search_key
        val listHtml = client.fetchListScheduleHtml()
        val searchKey = SmtuWebParser.extractSearchKey(listHtml)
            ?: throw IllegalStateException("Не удалось получить ключ формы поиска на сайте СПбГМТУ")

        // 2. Post search with session
        val searchResultHtml = client.searchTeacherHtml(searchKey, cleanQuery)
        SmtuWebParser.parseTeacherResults(searchResultHtml)
    }

    fun getSchedule(
        id: String,
        isTeacher: Boolean,
        forceRefresh: Boolean = false
    ): Flow<Resource<ScheduleResponse>> = flow {
        emit(Resource.Loading)
        val targetKey = if (isTeacher) "teacher_$id" else "group_$id"

        // 1. Check local Room cache first (instant offline response)
        val cached = dao.getCachedSchedule(targetKey)
        var cachedResponse: ScheduleResponse? = null
        if (cached != null) {
            try {
                cachedResponse = gson.fromJson(cached.jsonData, ScheduleResponse::class.java)
                emit(Resource.Success(data = cachedResponse, isOffline = true))
            } catch (e: Exception) {
                // Ignore parse error, proceed to network
            }
        }

        // 2. Fetch directly from smtu.ru
        try {
            val html = if (isTeacher) {
                client.fetchTeacherScheduleHtml(id)
            } else {
                client.fetchGroupScheduleHtml(id)
            }

            val parsedSchedule = SmtuWebParser.parseScheduleHtml(html, id, isTeacher)

            // Save to local cache
            val entity = CachedScheduleEntity(
                targetKey = targetKey,
                targetId = id,
                title = parsedSchedule.title,
                isTeacher = isTeacher,
                jsonData = gson.toJson(parsedSchedule),
                cachedAt = System.currentTimeMillis()
            )
            dao.insertCachedSchedule(entity)

            emit(Resource.Success(data = parsedSchedule, isOffline = false))
        } catch (e: Exception) {
            if (cachedResponse == null) {
                emit(Resource.Error(message = e.localizedMessage ?: "Ошибка загрузки расписания с сайта СПбГМТУ"))
            }
            // If cachedResponse exists, user already sees offline cache seamlessly
        }
    }.flowOn(Dispatchers.IO)

    suspend fun saveScheduleOffline(schedule: ScheduleResponse) = withContext(Dispatchers.IO) {
        val targetKey = if (schedule.isTeacher) "teacher_${schedule.id}" else "group_${schedule.id}"
        val entity = CachedScheduleEntity(
            targetKey = targetKey,
            targetId = schedule.id,
            title = schedule.title,
            isTeacher = schedule.isTeacher,
            jsonData = gson.toJson(schedule),
            cachedAt = System.currentTimeMillis()
        )
        dao.insertCachedSchedule(entity)

        // Also add to favorites
        dao.insertFavorite(
            FavoriteEntity(
                id = schedule.id,
                name = schedule.title,
                isTeacher = schedule.isTeacher,
                isDefault = false
            )
        )
    }

    fun getFavorites(): Flow<List<FavoriteEntity>> = dao.getFavorites()

    suspend fun getDefaultFavorite(): FavoriteEntity? = withContext(Dispatchers.IO) {
        dao.getDefaultFavorite()
    }

    suspend fun setDefaultGroup(groupId: String, groupName: String) = withContext(Dispatchers.IO) {
        dao.insertFavorite(
            FavoriteEntity(
                id = groupId,
                name = groupName,
                isTeacher = false,
                isDefault = true
            )
        )
        dao.setDefaultFavorite(groupId)
    }

    suspend fun toggleFavorite(id: String, name: String, isTeacher: Boolean) = withContext(Dispatchers.IO) {
        if (dao.isFavorite(id)) {
            dao.deleteFavorite(id)
        } else {
            dao.insertFavorite(
                FavoriteEntity(
                    id = id,
                    name = name,
                    isTeacher = isTeacher,
                    isDefault = false
                )
            )
        }
    }

    suspend fun isFavorite(id: String): Boolean = withContext(Dispatchers.IO) {
        dao.isFavorite(id)
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        dao.clearAllCachedSchedules()
    }
}

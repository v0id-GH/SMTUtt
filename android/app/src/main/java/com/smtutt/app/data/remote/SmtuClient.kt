package com.smtutt.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class SmtuClient {

    private val cookieStore = HashMap<String, List<Cookie>>()

    // In-memory CookieJar to maintain session (PHPSESSID) for teacher search
    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                .build()
            chain.proceed(request)
        }
        .build()

    companion object {
        const val BASE_URL = "https://www.smtu.ru"
    }

    suspend fun fetchListScheduleHtml(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/ru/listschedule/")
            .build()
        executeRequest(request)
    }

    suspend fun searchTeacherHtml(searchKey: String, surname: String): String = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder()
            .add("search_key", searchKey)
            .add("whatsearch", surname.trim())
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/ru/searchschedule/")
            .post(formBody)
            .header("Referer", "$BASE_URL/ru/listschedule/")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        executeRequest(request)
    }

    suspend fun fetchGroupScheduleHtml(groupId: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/ru/viewschedule_new/$groupId/")
            .build()
        executeRequest(request)
    }

    suspend fun fetchTeacherScheduleHtml(teacherId: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/ru/viewschedule_new/teacher/$teacherId/")
            .build()
        executeRequest(request)
    }

    private fun executeRequest(request: Request): String {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Ошибка сервера СПбГМТУ: HTTP ${response.code}")
            }
            return response.body?.string() ?: throw IOException("Пустой ответ от сервера СПбГМТУ")
        }
    }
}

package com.smtutt.app

import android.app.Application
import com.smtutt.app.data.local.AppDatabase
import com.smtutt.app.data.remote.SmtuClient
import com.smtutt.app.data.repository.ScheduleRepository

class SmtuApplication : Application() {

    lateinit var repository: ScheduleRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        val smtuClient = SmtuClient()
        repository = ScheduleRepository(smtuClient, database.scheduleDao())
    }
}

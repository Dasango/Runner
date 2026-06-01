package com.runner.app

import android.app.Application
import com.runner.app.data.RunnerDatabase
import com.whl.quickjs.android.QuickJSLoader

class RunnerApp : Application() {
    val database: RunnerDatabase by lazy { RunnerDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        QuickJSLoader.init()
    }
}

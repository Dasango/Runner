package com.runner.app

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.runner.app.data.RunnerDatabase
import com.runner.app.script.ScriptExecutionService
import com.whl.quickjs.android.QuickJSLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RunnerApp : Application() {
    val database: RunnerDatabase by lazy { RunnerDatabase.get(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        QuickJSLoader.init()

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Pequeño delay para asegurar que la conexión es estable antes de disparar scripts
                appScope.launch(Dispatchers.IO) {
                    delay(2000)
                    ScriptExecutionService.checkAndRunPendingFallbacks(this@RunnerApp)
                }
            }
        })
    }
}

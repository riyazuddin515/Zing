package com.riyazuddin.zing.workers

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.riyazuddin.zing.services.ZingFirebaseMessagingService

class ServiceChecker(
    context: Context,
    params: WorkerParameters
) :
    Worker(context, params) {

    companion object{
        val TAG: String = ServiceChecker::class.java.name
    }

    override fun doWork(): Result {
        return try {
            if (!isMyServiceRunning(ZingFirebaseMessagingService::class.java)) {
                val intent = Intent(applicationContext, ZingFirebaseMessagingService::class.java)
                applicationContext.startService(intent)
                Log.i(TAG, "doWork: service not running")
                if (isMyServiceRunning(ZingFirebaseMessagingService::class.java))
                    Log.i(TAG, "doWork: service started")
            } else {
                Log.i(TAG, "doWork: service running")
            }
            Result.success()
        }catch (e: Exception){
            Log.e(TAG, "doWork: Worker exception: ", e)
            Result.failure()
        }

    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager =
            applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        for (service in manager!!.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
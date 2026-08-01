package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.service.CrawlerService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d("BootReceiver", "Received boot or system action: $action")
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            try {
                Log.d("BootReceiver", "Auto-starting persistent CrawlerService after boot/update")
                CrawlerService.startService(context)
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to start CrawlerService on boot", e)
            }
        }
    }
}

package com.duyvv.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.duyvv.service.service.MusicService
import com.duyvv.service.utils.ACTION_MUSIC
import com.duyvv.service.utils.KEY_ACTION

class MusicReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.getIntExtra(ACTION_MUSIC, 0)
        val intentService = Intent(context, MusicService::class.java)
        intentService.putExtra(KEY_ACTION, action)
        context?.startService(intentService)
    }
}
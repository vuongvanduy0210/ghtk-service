package com.duyvv.service.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.duyvv.service.MainActivity
import com.duyvv.service.R
import com.duyvv.service.domain.MusicAction
import com.duyvv.service.domain.Song
import com.duyvv.service.receiver.MusicReceiver
import com.duyvv.service.utils.ACTION_MUSIC
import com.duyvv.service.utils.CHANNEL_ID

class MusicNotificationManager(
    private val context: Context,
    private val channelId: String,
    private val songs: List<Song>,
    private val currentSongIndex: Int,
    private val isPlaying: Boolean
) {

    private fun createPendingIntent(action: Int): PendingIntent {
        val intent = Intent(context, MusicReceiver::class.java).apply {
            putExtra(ACTION_MUSIC, action)
        }
        return PendingIntent.getBroadcast(
            context, action, intent,
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val activityPendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT
        else
            PendingIntent.FLAG_IMMUTABLE
    )


    fun buildNotification(): Notification {
        val notificationLayout = RemoteViews(context.packageName, R.layout.notification_music)
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
        val currentSong = songs[currentSongIndex]
        notificationLayout.apply {
            setTextViewText(R.id.tvNotificationSongName, currentSong.name)
            setTextViewText(R.id.tvNotificationSinger, currentSong.singer)

            setImageViewResource(
                R.id.btnNotificationPlayPause,
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
            setOnClickPendingIntent(
                R.id.btnNotificationPlayPause,
                if (isPlaying)
                    createPendingIntent(MusicAction.PAUSE)
                else
                    createPendingIntent(MusicAction.RESUME)

            )
            setOnClickPendingIntent(
                R.id.btnNotificationPrevious,
                createPendingIntent(MusicAction.PREVIOUS)
            )
            setOnClickPendingIntent(
                R.id.btnNotificationNext,
                createPendingIntent(MusicAction.NEXT)
            )
            setOnClickPendingIntent(
                R.id.btnNotificationVolumeDown,
                createPendingIntent(MusicAction.VOLUME_DOWN)
            )
            setOnClickPendingIntent(
                R.id.btnNotificationVolumeUp,
                createPendingIntent(MusicAction.VOLUME_UP)
            )
            setOnClickPendingIntent(
                R.id.btnCancel,
                createPendingIntent(MusicAction.CANCEL)
            )
        }
        return notificationBuilder.setSmallIcon(R.drawable.ic_music)
            .setSound(null)
            .setContentIntent(activityPendingIntent)
            .setOngoing(true)
            .setCustomBigContentView(notificationLayout)
            .build()
    }
}
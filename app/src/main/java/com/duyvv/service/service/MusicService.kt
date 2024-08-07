package com.duyvv.service.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.duyvv.service.MainActivity
import com.duyvv.service.R
import com.duyvv.service.domain.MusicAction
import com.duyvv.service.domain.Song
import com.duyvv.service.receiver.MusicReceiver
import com.duyvv.service.utils.ACTION_MUSIC
import com.duyvv.service.utils.CHANNEL_ID
import com.duyvv.service.utils.KEY_ACTION
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Timer
import java.util.TimerTask

class MusicService : Service(), MediaPlayer.OnCompletionListener {

    inner class MusicBinder : Binder() {
        fun getService() = this@MusicService
    }

    private val binder = MusicBinder()

    var mediaPlayer: MediaPlayer? = null

    private val songs = mutableListOf<Song>()

    private val _currentSongIndex = MutableStateFlow(0)
    val currentSongIndex = _currentSongIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentProcess = MutableStateFlow(0)
    val currentProcess = _currentProcess.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration = _duration.asStateFlow()

    private val _currentVolume = MutableStateFlow(1f)
    val currentVolume = _currentVolume.asStateFlow()

    private val _isActive = MutableStateFlow(true)
    val isActive = _isActive.asStateFlow()

    private var timer: Timer? = null

    fun setSongs(list: List<Song>) {
        songs.clear()
        songs.addAll(list)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MusicService", "onCreate")
        _isActive.value = true
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d("MusicService", "onBind")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MusicService", "onStartCommand")
        intent?.getIntExtra(KEY_ACTION, 0)?.let { actionValue ->
            if (actionValue != 0) {
                handleMusicAction(actionValue)
            }
        }
        return START_STICKY
    }

    private fun handleMusicAction(actionValue: Int) {
        when (actionValue) {
            MusicAction.RESUME -> {
                resumeMusic()
            }

            MusicAction.PAUSE -> {
                pauseMusic()
            }

            MusicAction.PREVIOUS -> {
                previousSong()
            }

            MusicAction.NEXT -> {
                nextSong()
            }

            MusicAction.VOLUME_DOWN -> {
                decreaseVolume()
            }

            MusicAction.VOLUME_UP -> {
                increaseVolume()
            }

            MusicAction.CANCEL -> {
                stopMusic()
            }
        }
    }

    private fun stopMusic() {
        _isActive.value = false
        _isPlaying.value = false
        stopSelf()
    }

    private fun sendNotification() {
        val notificationLayout = RemoteViews(packageName, R.layout.notification_music)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
        val currentSong = songs[currentSongIndex.value]
        notificationLayout.apply {
            setTextViewText(R.id.tvNotificationSongName, currentSong.name)
            setTextViewText(R.id.tvNotificationSinger, currentSong.singer)

            setImageViewResource(
                R.id.btnNotificationPlayPause,
                if (isPlaying.value) R.drawable.ic_pause else R.drawable.ic_play
            )
            setOnClickPendingIntent(
                R.id.btnNotificationPlayPause,
                if (isPlaying.value)
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
        notificationBuilder.setSmallIcon(R.drawable.ic_music)
            .setSound(null)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                        PendingIntent.FLAG_UPDATE_CURRENT
                    else
                        PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .setCustomBigContentView(notificationLayout)

        val notification = notificationBuilder.build()
        startForeground(1, notification)
    }

    fun startMusic() {
        if (mediaPlayer != null) {
            mediaPlayer?.release()
            mediaPlayer = null
        }

        val currentSong = songs[currentSongIndex.value]
        mediaPlayer = MediaPlayer.create(this, currentSong.resourceId).apply {
            setOnCompletionListener(this@MusicService)
            start()
            _duration.value = duration
            setVolume(currentVolume.value, currentVolume.value)
        }
        sendCurrentProcess()
        _isPlaying.value = true
        sendNotification()
    }

    fun pauseMusic() {
        if (mediaPlayer != null && isPlaying.value) {
            mediaPlayer!!.pause()
            _isPlaying.value = false
            timer?.cancel()
        }
        sendNotification()
    }

    fun resumeMusic() {
        if (mediaPlayer != null && !isPlaying.value) {
            mediaPlayer!!.start()
            _isPlaying.value = true
            sendCurrentProcess()
        }
        sendNotification()
    }

    fun previousSong() {
        _currentSongIndex.value = (currentSongIndex.value - 1 + songs.size) % songs.size
        startMusic()
    }

    fun nextSong() {
        _currentSongIndex.value = (currentSongIndex.value + 1) % songs.size
        startMusic()
    }

    private fun sendCurrentProcess() {
        if (timer != null) {
            timer?.cancel()
            timer = null
        }

        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                try {
                    mediaPlayer?.let {
                        _currentProcess.value = it.currentPosition
                    }
                } catch (e: IllegalStateException) {
                    Log.d("TAG", "${e.message}")
                }
            }
        }, 0, 1000)
    }

    fun updateCurrentProcess(currentProcess: Int) {
        mediaPlayer?.seekTo(currentProcess)
    }

    fun updateCurrentVolume(currentProcess: Int) {
        _currentVolume.value = currentProcess / 100f
        mediaPlayer?.setVolume(currentVolume.value, currentVolume.value)
    }

    override fun onCompletion(mp: MediaPlayer?) {
        nextSong()
    }

    fun increaseVolume() {
        _currentVolume.value =
            if (currentVolume.value + 0.1f > 1.0f) 1.0f else (currentVolume.value + 0.1f)
        mediaPlayer?.setVolume(currentVolume.value, currentVolume.value)
    }

    fun decreaseVolume() {
        _currentVolume.value =
            if (currentVolume.value - 0.1f < 0f) 0f else (currentVolume.value - 0.1f)
        mediaPlayer?.setVolume(currentVolume.value, currentVolume.value)
    }

    private fun createPendingIntent(action: Int): PendingIntent {
        val intent = Intent(this@MusicService, MusicReceiver::class.java)
        intent.putExtra(ACTION_MUSIC, action)
        return PendingIntent.getBroadcast(
            applicationContext,
            action,
            intent,
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        timer?.cancel()
        timer = null
    }
}
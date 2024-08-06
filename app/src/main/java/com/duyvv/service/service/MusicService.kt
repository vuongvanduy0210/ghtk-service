package com.duyvv.service.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.duyvv.service.domain.Song
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

    private var timer: Timer? = null

    fun setSongs(list: List<Song>) {
        songs.clear()
        songs.addAll(list)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
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
            sendCurrentProcess()
            _isPlaying.value = true
            setVolume(currentVolume.value, currentVolume.value)
        }
    }

    fun pauseMusic() {
        if (mediaPlayer != null && isPlaying.value) {
            mediaPlayer!!.pause()
            _isPlaying.value = false
            timer?.cancel()
        }
    }

    fun resumeMusic() {
        if (mediaPlayer != null && !isPlaying.value) {
            mediaPlayer!!.start()
            _isPlaying.value = true
            sendCurrentProcess()
        }
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

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        timer?.cancel()
        timer = null
    }
}
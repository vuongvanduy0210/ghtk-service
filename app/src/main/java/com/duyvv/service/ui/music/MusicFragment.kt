package com.duyvv.service.ui.music

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.duyvv.service.MainActivity
import com.duyvv.service.R
import com.duyvv.service.base.BaseFragment
import com.duyvv.service.databinding.FragmentMusicPlayerBinding
import com.duyvv.service.domain.Song
import com.duyvv.service.utils.toMinuteSecond

class MusicFragment : BaseFragment<FragmentMusicPlayerBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentMusicPlayerBinding.inflate(inflater, container, false)

    override fun init() {
        activity = requireActivity() as MainActivity
    }

    private val viewModel: MusicViewModel by viewModels()

    private lateinit var activity: MainActivity

    private val musicService by lazy { activity.musicService }

    private lateinit var runnable: Runnable

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setup()
    }

    private fun setup() {
        checkPermission()

        activity.startMusicService()

        collectLifecycleFlow(activity.isBound) { isBound ->
            if (isBound) {
                setupCollect()

                setupListener()
            }
        }
    }

    private fun checkPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !activity.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            activity.requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupListener() {
        binding.imgNext.setOnClickListener {
            musicService?.nextSong()
        }

        binding.imgPrevious.setOnClickListener {
            musicService?.previousSong()
        }

        binding.imgPlay.setOnClickListener {
            if (musicService?.isPlaying?.value == true) {
                musicService?.pauseMusic()
            } else {
                musicService?.resumeMusic()
            }
        }

        binding.seekBarMusic.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService?.updateCurrentProcess(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnVolumeUp.setOnClickListener {
            musicService?.increaseVolume()
        }

        binding.btnVolumeDown.setOnClickListener {
            musicService?.decreaseVolume()
        }

        binding.seekBarVolume.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService?.updateCurrentVolume(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupCollect() {
        collectLifecycleFlow(viewModel.songs) {
            musicService?.setSongs(it)
            if (musicService?.mediaPlayer != null) {
                musicService?.resumeMusic()
            } else {
                musicService?.startMusic()
            }
        }

        collectLifecycleFlow(musicService!!.currentSongIndex) {
            updateUI(viewModel.songs.value[it])
        }

        collectLifecycleFlow(musicService!!.isPlaying) {
            binding.imgPlay.setImageResource(
                if (it) R.drawable.ic_pause else R.drawable.ic_play
            )
            if (it) startRotateSongImage() else stopRotateSongImage()
        }

        collectLifecycleFlow(musicService!!.currentProcess) { currentTime ->
            updateCurrentTime(currentTime)
        }

        collectLifecycleFlow(musicService!!.duration) { duration ->
            updateDuration(duration)
        }

        collectLifecycleFlow(musicService!!.currentVolume) { currentVolume ->
            updateVolumeSeekbar(currentVolume)
        }

        collectLifecycleFlow(musicService!!.isActive) { isActive ->
            if (!isActive) {
                activity.closeMusic()
                findNavController().popBackStack()
            }
        }
    }

    private fun updateVolumeSeekbar(currentVolume: Float) {
        binding.seekBarVolume.progress = (currentVolume * 100).toInt()
    }

    private fun updateCurrentTime(currentTime: Int) {
        binding.seekBarMusic.progress = currentTime
        binding.tvCurrentTime.text = currentTime.toMinuteSecond()
    }

    private fun updateDuration(duration: Int) {
        binding.seekBarMusic.max = duration
        binding.tvFinalTime.text = duration.toMinuteSecond()
    }

    private fun updateUI(currentSong: Song) {
        binding.apply {
            tvSongName.text = currentSong.name
            tvSinger.text = currentSong.singer
            imgSong.setImageResource(R.drawable.music_icon)
        }
    }

    private fun startRotateSongImage() {
        runnable = Runnable {
            binding.imgSong
                .animate().rotationBy(360f)
                .withEndAction(runnable).setDuration(10000)
                .setInterpolator(LinearInterpolator())
                .start()
        }
        runnable.run()
    }

    private fun stopRotateSongImage() {
        binding.imgSong.animate().cancel()
    }
}
package com.example.audioapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.audioapp.model.Song
import java.io.File
import java.util.concurrent.TimeUnit

class Song : AppCompatActivity() {
    private var seekBar: SeekBar? = null
    private var songTime: TextView? = null
    private var songTitle: TextView? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentSongIndex = 0
    private var songs: List<Song> = emptyList()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song)

        // Request runtime permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }

        // Set up folder path and retrieve songs
        val folderPath = "/storage/emulated/0/Songs/"
        songs = getSongsFromFolder(folderPath)

        // UI components
        seekBar = findViewById(R.id.progressBar)
        songTime = findViewById(R.id.songDuration)
        songTitle = findViewById(R.id.songTitle)
        val btnPlayPause = findViewById<ImageButton>(R.id.btnPlayPause)
        val btnNext = findViewById<ImageButton>(R.id.btnNext)
        val btnPrev = findViewById<ImageButton>(R.id.btnPrev)

        if (songs.isNotEmpty()) {
            // Play the first song on startup
            playSong(songs[currentSongIndex])
        } else {
            Toast.makeText(this, "No songs found in the folder", Toast.LENGTH_SHORT).show()
        }

        btnPlayPause?.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                btnPlayPause?.setImageResource(R.drawable.play_buttton)
            } else {
                mediaPlayer?.start()
                btnPlayPause?.setImageResource(R.drawable.pause)
                updateSeekBar()
            }
        }

        btnNext?.setOnClickListener {
            currentSongIndex = (currentSongIndex + 1) % songs.size
            playSong(songs[currentSongIndex])
            btnPlayPause?.setImageResource(R.drawable.pause)
        }

        btnPrev?.setOnClickListener {
            currentSongIndex = if (currentSongIndex == 0) songs.size - 1 else currentSongIndex - 1
            playSong(songs[currentSongIndex])
            btnPlayPause?.setImageResource(R.drawable.pause)
        }

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun getSongsFromFolder(folderPath: String): List<Song> {
        val folder = File(folderPath)
        if (folder.exists()) {
            val songFiles = folder.listFiles { file -> file.extension == "mp3" }
            return songFiles?.map { file -> Song(file.nameWithoutExtension, file.absolutePath) } ?: emptyList()
        } else {
            Toast.makeText(this, "Directory not found: $folderPath", Toast.LENGTH_SHORT).show()
            return emptyList()
        }
    }

    private fun playSong(song: Song) {
        mediaPlayer?.stop()
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.path)
            prepare()
            start()
        }

        // Update UI with the current song details
        songTitle?.text = song.title
        seekBar?.max = mediaPlayer?.duration ?: 0
        updateSeekBar()
    }

    private fun updateSeekBar() {
        mediaPlayer?.let {
            seekBar?.progress = it.currentPosition
            songTime?.text = formatTime(it.currentPosition) + " / " + formatTime(it.duration)
            if (it.isPlaying) {
                handler.postDelayed({ updateSeekBar() }, 1000)
            }
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        handler.removeCallbacksAndMessages(null)
    }
}

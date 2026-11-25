package com.noorehidayah.quran.utils

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AudioPlaybackState(
    val activeAyahId: String? = null,
    val isPlaying: Boolean = false,
    val isDownloading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Offline-first Quran Audio Tilawat player (Mishary Rashid Alafasy recitation).
 * Checks local offline cache first; if not cached, downloads once into app storage
 * so future playback works 100% offline.
 */
object QuranAudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private fun audioFile(context: Context, surahNumber: Int, ayahNumber: Int): File {
        val dir = File(context.filesDir, "audio")
        if (!dir.exists()) dir.mkdirs()
        val name = "%03d%03d.mp3".format(surahNumber, ayahNumber)
        return File(dir, name)
    }

    fun isCachedOffline(context: Context, surahNumber: Int, ayahNumber: Int): Boolean {
        val file = audioFile(context, surahNumber, ayahNumber)
        return file.exists() && file.length() > 1024L
    }

    suspend fun togglePlayAyah(
        context: Context,
        ayahId: String,
        surahNumber: Int,
        ayahNumber: Int
    ) {
        val current = _state.value
        if (current.activeAyahId == ayahId && current.isPlaying) {
            stop()
            return
        }

        stop()
        val file = audioFile(context, surahNumber, ayahNumber)
        if (!file.exists() || file.length() <= 1024L) {
            _state.value = AudioPlaybackState(activeAyahId = ayahId, isDownloading = true)
            val ok = downloadAyahAudio(file, surahNumber, ayahNumber)
            if (!ok) {
                _state.value = AudioPlaybackState(
                    activeAyahId = null,
                    errorMessage = "Audio offline cached nahi hai. Ek baar internet on karke Play dabayein."
                )
                return
            }
        }

        try {
            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    _state.value = AudioPlaybackState()
                }
                setOnErrorListener { _, _, _ ->
                    _state.value = AudioPlaybackState(errorMessage = "Audio playback error.")
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = mp
            _state.value = AudioPlaybackState(activeAyahId = ayahId, isPlaying = true)
        } catch (e: Exception) {
            _state.value = AudioPlaybackState(errorMessage = e.localizedMessage ?: "Audio error")
        }
    }

    suspend fun downloadSurahAudio(
        context: Context,
        surahNumber: Int,
        ayahNumbers: List<Int>
    ): Int = withContext(Dispatchers.IO) {
        var downloaded = 0
        for (ayahNum in ayahNumbers) {
            val file = audioFile(context, surahNumber, ayahNum)
            if (file.exists() && file.length() > 1024L) {
                downloaded++
            } else if (downloadAyahAudio(file, surahNumber, ayahNum)) {
                downloaded++
            }
        }
        downloaded
    }

    private suspend fun downloadAyahAudio(
        targetFile: File,
        surahNumber: Int,
        ayahNumber: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val code = "%03d%03d.mp3".format(surahNumber, ayahNumber)
        val urlStr = "https://everyayah.com/data/Alafasy_128kbps/$code"
        try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 12000
            }
            if (conn.responseCode == 200) {
                val temp = File(targetFile.parentFile, "${code}.tmp")
                conn.inputStream.use { input ->
                    temp.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (temp.length() > 1024L) {
                    temp.renameTo(targetFile)
                    return@withContext true
                }
                temp.delete()
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        _state.value = AudioPlaybackState()
    }
}

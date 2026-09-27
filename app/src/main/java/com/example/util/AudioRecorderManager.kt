package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

object AudioRecorderManager {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTimeMs: Long = 0L

    fun startRecording(context: Context): File? {
        stopAndRelease()
        try {
            val file = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
            currentFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            // Use MIC / VOICE_COMMUNICATION audio source for high-gain, loud & crisp voice recording
            try {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            } catch (e: Exception) {
                try {
                    recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                } catch (_: Exception) {
                    recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                }
            }

            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(192000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)

            recorder.prepare()
            recorder.start()
            startTimeMs = System.currentTimeMillis()
            mediaRecorder = recorder
            Log.d("AudioRecorderManager", "High-Volume Crisp Recording started at ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Failed to start recording: ${e.message}", e)
            stopAndRelease()
            currentFile?.delete()
            currentFile = null
            return null
        }
    }

    fun currentAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude?.coerceIn(0, 32767) ?: 0
        } catch (_: Throwable) {
            0
        }
    }

    fun stopRecording(): Pair<File?, Int> {
        val file = currentFile
        var durationSecs = 0
        val elapsedMs = if (startTimeMs > 0) System.currentTimeMillis() - startTimeMs else 0L
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            durationSecs = (elapsedMs / 1000L).toInt()
            // Strict 1 second minimum rule: if less than 1000ms, discard recording
            if (elapsedMs < 1000L) {
                file?.delete()
                return Pair(null, 0)
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error stopping recorder: ${e.message}")
            file?.delete()
            stopAndRelease()
            return Pair(null, 0)
        } finally {
            mediaRecorder = null
            currentFile = null
            startTimeMs = 0L
        }
        return Pair(file, durationSecs)
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.w("AudioRecorderManager", "Error cancelling recorder: ${e.message}")
        } finally {
            mediaRecorder = null
            currentFile?.delete()
            currentFile = null
            startTimeMs = 0L
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Error pausing recorder: ${e.message}")
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Error resuming recorder: ${e.message}")
            }
        }
    }

    private fun stopAndRelease() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
    }
}

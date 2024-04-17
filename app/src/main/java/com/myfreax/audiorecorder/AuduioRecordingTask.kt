package com.myfreax.audiorecorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.CoroutineContext

class AudioRecordingTask(context: Context, mediaProjection: MediaProjection) : CoroutineScope {

    companion object {
        const val TAG = "AudioRecordingTask"
    }
    private var running:Boolean = false
    private var audioRecord: AudioRecord? = null
    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job


    init {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            audioRecord =
                AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC) // Use the microphone as the source
                    .setAudioFormat(
                        AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(44100)
                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(2 * 1024 * 1024)
                    .build()
        } else {
            throw SecurityException("Permission Denied: RECORD_AUDIO")
        }
    }


    fun execute(fileOutputStream: FileOutputStream) = launch {
        Log.d(TAG, "AudioRecord Start Recording")
        running = true
        audioRecord?.startRecording()
        try {
            withContext(Dispatchers.IO) {
                while (running) {
                    val byteArray = ByteArray(1024)
                    val readResult = audioRecord?.read(byteArray, 0, byteArray.size) ?: -1
                    if (readResult > 0) {
                        try {
                            fileOutputStream.write(byteArray, 0, readResult)
                        } catch (e: IOException) {
                            Log.e(TAG, "Error writing to file output stream", e)
                            break // Exit the loop or handle otherwise
                        }
                    }
                }
            }
        } finally {
            try {
                fileOutputStream.flush()
                fileOutputStream.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing file output stream", e)
            }
        }
    }
    fun cancel() = runBlocking {
        running = false
        job.cancelAndJoin()  // Make sure to wait until the job is fully cancelled
    }



}
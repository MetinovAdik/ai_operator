package com.myfreax.audiorecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import androidx.activity.result.ActivityResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.IOException

class AudioRecordService : Service() {

    private lateinit var audioCaptureFile: File
    private lateinit var mp3File: File
    private lateinit var fileOutputStream: FileOutputStream
    private lateinit var audioRecordingTask: AudioRecordingTask

    companion object {
        private lateinit var activityResult: ActivityResult
        const val TAG = "AudioRecordService"
        const val NOTIFICATION_ID = 441823
        const val NOTIFICATION_CHANNEL_ID = "com.myfreax.webrtc.app"
        const val NOTIFICATION_CHANNEL_NAME = "com.myfreax.webrtc.app"

        fun start(context: Context, mediaProjectionActivityResult: ActivityResult) {
            activityResult = mediaProjectionActivityResult
            val intent = Intent(context, AudioRecordService::class.java)
            context.startForegroundService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotification()
        if (!::fileOutputStream.isInitialized) {  // Check if the stream is already open
            prepareRecording()
        }
        startRecording()
        return START_STICKY
    }

    private fun prepareRecording() {
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = mediaProjectionManager.getMediaProjection(
            activityResult.resultCode,
            activityResult.data!!
        )
        audioRecordingTask = AudioRecordingTask(this, mediaProjection)

        val audioCapturesDirectory = File(getExternalFilesDir(null), "/AudioCaptures")
        if (!audioCapturesDirectory.exists()) {
            audioCapturesDirectory.mkdirs()
        }
        val timestamp = SimpleDateFormat("dd-MM-yyyy-hh-mm-ss", Locale.US).format(Date())
        audioCaptureFile = File(audioCapturesDirectory, "Capture-$timestamp.pcm")
        mp3File = File(audioCapturesDirectory, "Capture-$timestamp.mp3")
        fileOutputStream = FileOutputStream(audioCaptureFile)
    }

    private fun startRecording() {
        audioRecordingTask.execute(fileOutputStream)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()  // Wait for the recording to stop properly
        if (::fileOutputStream.isInitialized) {
            synchronized(fileOutputStream) {
                try {
                    fileOutputStream.flush()
                    fileOutputStream.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing file output stream", e)
                }
            }
        }
        convertToMp3()  // Convert to MP3 only after all resources are cleaned up
    }

    private fun convertToMp3() {
        if (audioCaptureFile.exists()) {
            val cmd = "-y -f s16le -ar 44100 -ac 1 -i ${audioCaptureFile.absolutePath} -acodec libmp3lame ${mp3File.absolutePath}"
            val result = FFmpeg.execute(cmd)
            if (result == 0) {  // Check if FFmpeg successfully executed
                audioCaptureFile.delete()  // Delete PCM file after successful conversion
                Log.d(TAG, "PCM file deleted successfully")
                startUploadService(mp3File.absolutePath)  // Start upload service with path to MP3
            } else {
                Log.e(TAG, "Failed to convert PCM to MP3, keeping the PCM file")
            }
        } else {
            Log.e(TAG, "PCM file does not exist")
        }
    }

    private fun startUploadService(filePath: String) {
        val uploadIntent = Intent(this, AsrUploadService::class.java).apply {
            putExtra("file_path", filePath)
        }
        startService(uploadIntent)
    }


    private fun stopRecording() {
        audioRecordingTask.cancel() // This should stop the recording
        stopSelf() // This stops the service itself
    }

    private fun createNotification() {
        createNotificationChannel()
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_record_voice_over_24)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.recording))
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setShowWhen(true)
            .build()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

package com.example.mp4tomp3converter

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.mp4tomp3converter.databinding.ActivityMainBinding
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.ExecuteCallback
import java.io.File
import java.io.FileOutputStream
import android.app.AlertDialog
import android.content.ContentUris

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var selectVideoLauncher: ActivityResultLauncher<Intent>
    private var selectedVideoUri: Uri? = null
    private var audioQuality: String = "320k"
    private var filename: String = "converted_${System.currentTimeMillis()}.mp3"

    private val CHANNEL_ID = "MP4toMP3_CHANNEL"
    private val NOTIFICATION_ID = 1
    private val REQUEST_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()

        // Check and request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSION_CODE)
        }

        binding.selectButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            selectVideoLauncher.launch(intent)
        }

        binding.selectQualityButton.setOnClickListener {
            // Open dialog or activity to select audio quality
            showQualitySelectionDialog()
        }

        binding.selectFilenameButton.setOnClickListener {
            // Open dialog to input filename
            showFilenameInputDialog()
        }

        setupActivityResultLaunchers()
    }

    private fun setupActivityResultLaunchers() {
        selectVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedVideoUri = uri
                    generateOutputPath()
                } ?: showToast("No video selected")
            }
        }
    }

    private fun generateOutputPath() {
        val outputDirectory = File("/storage/emulated/0/Music/MP4toMP3")
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs() // Create directory if it doesn't exist
        }

        val outputFile = File(outputDirectory, filename)
        convertMp4ToMp3(selectedVideoUri!!, Uri.fromFile(outputFile))
    }

    private fun convertMp4ToMp3(inputUri: Uri, outputUri: Uri) {
        val inputPath = getFilePathFromUri(inputUri) ?: run {
            showToast("Invalid input URI")
            return
        }

        val outputPath = outputUri.path ?: run {
            showToast("Invalid output URI")
            return
        }

        val outputFile = File(outputPath)
        if (outputFile.exists()) {
            outputFile.delete() // Remove the existing file if necessary
        }

        val command = "-i $inputPath -b:a $audioQuality $outputPath"

        binding.conversionProgress.visibility = android.view.View.VISIBLE

        FFmpeg.executeAsync(command, object : ExecuteCallback {
            override fun apply(executionId: Long, returnCode: Int) {
                runOnUiThread {
                    binding.conversionProgress.visibility = android.view.View.GONE
                    val message = if (returnCode == 0) "Conversion successful! File saved at: $outputPath" else "Conversion failed"
                    showToast(message)
                    sendNotification(message)
                }
            }
        })
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        return try {
            val tempFile = File.createTempFile("temp", ".mp3", cacheDir)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            tempFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showQualitySelectionDialog() {
        val qualityOptions = arrayOf("128k", "192k", "256k", "320k")
        val checkedItem = qualityOptions.indexOf(audioQuality)

        AlertDialog.Builder(this)
            .setTitle("Select Audio Quality")
            .setSingleChoiceItems(qualityOptions, checkedItem) { _, which ->
                audioQuality = qualityOptions[which]
            }
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                showToast("Selected Quality: $audioQuality")
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }


    private fun showFilenameInputDialog() {
        val editText = EditText(this)
        editText.inputType = InputType.TYPE_CLASS_TEXT

        AlertDialog.Builder(this)
            .setTitle("Enter Filename")
            .setView(editText)
            .setPositiveButton("OK") { dialog, _ ->
                val inputFilename = editText.text.toString().trim()
                if (inputFilename.isNotEmpty()) {
                    filename = "${inputFilename}.mp3"
                    showToast("Filename set to: $filename")
                } else {
                    showToast("Filename cannot be empty")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MP4toMP3 Notifications"
            val descriptionText = "Notifications for MP4 to MP3 conversion status"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification(message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE // Add this flag
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher) // Replace with your notification icon
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher)) // Replace with your app icon
            .setContentTitle("MP4 to MP3 Conversion")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions granted, proceed with the action
            } else {
                showToast("Permissions denied. Cannot access files.")
            }
        }
    }
}
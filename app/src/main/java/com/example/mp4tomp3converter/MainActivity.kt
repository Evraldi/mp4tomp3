package com.example.mp4tomp3converter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mp4tomp3converter.databinding.ActivityMainBinding
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.ExecuteCallback
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var createFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectVideoLauncher: ActivityResultLauncher<Intent>
    private var selectedVideoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActivityResultLaunchers()

        binding.selectButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            selectVideoLauncher.launch(intent)
        }
    }

    private fun setupActivityResultLaunchers() {
        createFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { outputUri ->
                    selectedVideoUri?.let { inputUri ->
                        convertMp4ToMp3(inputUri, outputUri)
                    } ?: showToast("No video selected")
                } ?: showToast("Failed to create output file")
            }
        }

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

        val outputFileName = "converted_${System.currentTimeMillis()}.mp3"
        val outputFile = File(outputDirectory, outputFileName)

        convertMp4ToMp3(selectedVideoUri!!, Uri.fromFile(outputFile))
    }

    private fun convertMp4ToMp3(inputUri: Uri, outputUri: Uri) {
        val inputPath = getFilePathFromUri(inputUri) ?: run {
            Toast.makeText(this, "Invalid input URI", Toast.LENGTH_SHORT).show()
            return
        }

        val outputPath = outputUri.path ?: run {
            Toast.makeText(this, "Invalid output URI", Toast.LENGTH_SHORT).show()
            return
        }

        val outputFile = File(outputPath)
        if (outputFile.exists()) {
            outputFile.delete() // Remove the existing file if necessary
        }

        val command = "-i $inputPath -b:a 320k $outputPath"

        FFmpeg.executeAsync(command, object : ExecuteCallback {
            override fun apply(executionId: Long, returnCode: Int) {
                runOnUiThread {
                    if (returnCode == 0) {
                        Toast.makeText(this@MainActivity, "Conversion successful! File saved at: $outputPath", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Conversion failed", Toast.LENGTH_SHORT).show()
                    }
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

    private fun copyUriToTempFile(uri: Uri): String? {
        val tempFile = File.createTempFile("temp", ".mp4", cacheDir)
        return try {
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

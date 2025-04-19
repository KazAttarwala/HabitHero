package com.example.habithero.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * A utility class for creating sound files. 
 * Note: This is a workaround for this example since we can't directly create binary files.
 * In a real app, you would add sound files directly to the raw resource directory.
 */
class SoundFileCreator {
    companion object {
        /**
         * Creates a minimalist success sound MP3 file in the app's raw directory.
         * This is a tiny MP3 file with a simple "ding" sound.
         */
        fun createSuccessSound(context: Context) {
            // Very simple MP3 file header bytes for a success "ding" sound
            // Note: This is a minimal placeholder MP3. In a real app, you should use a properly 
            // crafted sound file placed in res/raw directory
            val successSoundBytes = byteArrayOf(
                0xFF.toByte(), 0xFB.toByte(), 0x50.toByte(), 0x80.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
            )

            try {
                // Create a file in the app's raw directory
                val rawDir = File(context.filesDir, "raw")
                if (!rawDir.exists()) {
                    rawDir.mkdirs()
                }
                
                val soundFile = File(rawDir, "success_sound.mp3")
                val fos = FileOutputStream(soundFile)
                fos.write(successSoundBytes)
                fos.close()
                
                println("Created success sound file at: ${soundFile.absolutePath}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
} 

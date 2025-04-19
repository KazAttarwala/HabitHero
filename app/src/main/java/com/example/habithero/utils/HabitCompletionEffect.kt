package com.example.habithero.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import nl.dionsegijn.konfetti.xml.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import com.example.habithero.R
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Utility class for displaying visual and audio effects when a habit is completed
 */
class HabitCompletionEffect {
    companion object {
        private const val TAG = "HabitCompletionEffect"
        
        /**
         * Play a confetti animation from the bottom of the screen with a burst of particles
         * @param konfettiView The KonfettiView to display the animation on
         */
        fun playConfetti(konfettiView: KonfettiView) {
            val party = Party(
                speed = 0f,
                maxSpeed = 30f,
                damping = 0.9f,
                spread = 360,
                colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def).map { it },
                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
                position = Position.Relative(0.5, 0.8)
            )
            
            konfettiView.start(party)
        }

        /**
         * Play a success sound effect when a habit is completed
         * @param context Context to create the MediaPlayer
         */
        fun playSuccessSound(context: Context) {
            try {
                // Try to load from resources first
                val mediaPlayer = MediaPlayer()
                
                try {
                    // Attempt to create from the raw resource
                    val resourceId = R.raw::class.java.getField("success_sound").getInt(null)
                    mediaPlayer.setDataSource(context.resources.openRawResourceFd(resourceId))
                } catch (e: Exception) {
                    Log.w(TAG, "Could not load sound from resources: ${e.message}")
                    
                    // Try to use a file from app's private storage as a fallback
                    val soundFile = File(context.filesDir, "raw/success_sound.mp3")
                    if (soundFile.exists()) {
                        mediaPlayer.setDataSource(soundFile.absolutePath)
                    } else {
                        // If no sound found, just vibrate instead
                        Toast.makeText(context, "Habit completed! 🎉", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                
                mediaPlayer.prepare()
                mediaPlayer.setOnCompletionListener { mp ->
                    mp.release()
                }
                mediaPlayer.start()
            } catch (e: Exception) {
                // If sound playback fails for any reason, log and continue
                Log.e(TAG, "Error playing success sound", e)
            }
        }

        /**
         * Play both confetti animation and sound effect
         * @param context Context to create the MediaPlayer
         * @param konfettiView The KonfettiView to display the animation on
         */
        fun playCompletionEffects(context: Context, konfettiView: KonfettiView) {
            playConfetti(konfettiView)
            playSuccessSound(context)
        }
    }
} 
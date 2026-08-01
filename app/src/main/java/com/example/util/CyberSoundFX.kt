package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.ToneGenerator
import android.media.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object CyberSoundFX {

    private var toneGen: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    } catch (e: Exception) {
        null
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Futuristic Click / Beep
     */
    fun playClickSound() {
        scope.launch {
            try {
                toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
            } catch (e: Exception) {
                // Ignore fallback
            }
        }
    }

    /**
     * Cyber Radar Scan Pulse Tone
     */
    fun playScanPulseSound() {
        scope.launch {
            try {
                toneGen?.startTone(ToneGenerator.TONE_SUP_PIP, 60)
            } catch (e: Exception) {
                // Fallback
            }
        }
    }

    /**
     * Cyber Alert Tone
     */
    fun playAlertSound() {
        scope.launch {
            try {
                toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
            } catch (e: Exception) {
                // Fallback
            }
        }
    }

    /**
     * Futuristic Success Chime
     */
    fun playSuccessSound() {
        scope.launch {
            try {
                generatePcmTone(880, 50)
                generatePcmTone(1320, 70)
                generatePcmTone(1760, 100)
            } catch (e: Exception) {
                toneGen?.startTone(ToneGenerator.TONE_PROP_PROMPT, 100)
            }
        }
    }

    /**
     * High-Tech Glitch Chirp
     */
    fun playGlitchSound() {
        scope.launch {
            try {
                toneGen?.startTone(ToneGenerator.TONE_DTMF_D, 40)
            } catch (e: Exception) {
                // Fallback
            }
        }
    }

    private fun generatePcmTone(freqHz: Int, durationMs: Int) {
        val sampleRate = 22050
        val numSamples = durationMs * sampleRate / 1000
        val sample = DoubleArray(numSamples)
        val generatedSnd = ByteArray(2 * numSamples)

        for (i in 0 until numSamples) {
            sample[i] = sin(2.0 * Math.PI * i.toDouble() / (sampleRate.toDouble() / freqHz.toDouble()))
        }

        var idx = 0
        for (dVal in sample) {
            val shortVal = (dVal * 32767).toInt().toShort()
            generatedSnd[idx++] = (shortVal.toInt() and 0x00ff).toByte()
            generatedSnd[idx++] = (shortVal.toInt() and 0xff00 shr 8).toByte()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(generatedSnd.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(generatedSnd, 0, generatedSnd.size)
        track.play()
    }
}

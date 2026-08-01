package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

enum class CyberBgmTrack(val title: String, val tempoBpm: Int) {
    NEON_SYNTHWAVE("NEON SYNTHWAVE 120BPM", 120),
    CYBER_AMBINDEX("CYBER MATRIX DRONE", 90),
    RETRO_CYBERDECK("RETRO CYBERDECK BEAT", 128)
}

object CyberBgmSynthesizer {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow(CyberBgmTrack.NEON_SYNTHWAVE)
    val currentTrack: StateFlow<CyberBgmTrack> = _currentTrack.asStateFlow()

    private val _volume = MutableStateFlow(0.4f) // 0.0f to 1.0f
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)
    private var bgmJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private const val SAMPLE_RATE = 22050

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true

        bgmJob?.cancel()
        bgmJob = scope.launch {
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                val buffer = ShortArray(1024)
                var sampleIndex = 0L

                while (isActive && _isPlaying.value) {
                    val trackType = _currentTrack.value
                    val currentVol = _volume.value

                    for (i in buffer.indices) {
                        val sampleTime = sampleIndex.toDouble() / SAMPLE_RATE
                        val sampleValue = generateSynthSample(sampleTime, sampleIndex, trackType) * currentVol
                        val shortVal = (sampleValue.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
                        buffer[i] = shortVal
                        sampleIndex++
                    }

                    track.write(buffer, 0, buffer.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopAudioTrack()
            }
        }
    }

    fun pause() {
        _isPlaying.value = false
        bgmJob?.cancel()
        stopAudioTrack()
    }

    fun setVolume(vol: Float) {
        _volume.value = vol.coerceIn(0.0f, 1.0f)
    }

    fun setTrack(track: CyberBgmTrack) {
        _currentTrack.value = track
    }

    fun nextTrack() {
        val tracks = CyberBgmTrack.values()
        val nextIdx = (_currentTrack.value.ordinal + 1) % tracks.size
        _currentTrack.value = tracks[nextIdx]
    }

    private fun stopAudioTrack() {
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            audioTrack = null
        }
    }

    private fun generateSynthSample(t: Double, sampleIdx: Long, track: CyberBgmTrack): Double {
        return when (track) {
            CyberBgmTrack.NEON_SYNTHWAVE -> {
                // 120 BPM Arpeggiated Cyberpunk Synthwave
                val beatDuration = 60.0 / 120.0
                val eighthNote = beatDuration / 2.0
                val noteIndex = ((t / eighthNote).toInt()) % 8

                // Dark pentatonic synth arp notes in Hz: A2 (110), C3 (130.81), D3 (146.83), E3 (164.81), G3 (196), A3 (220)
                val arpFreqs = doubleArrayOf(110.0, 130.81, 164.81, 196.0, 220.0, 196.0, 164.81, 130.81)
                val targetFreq = arpFreqs[noteIndex]

                // Sub-bass drone: Low 55Hz A1 note with slow 0.25Hz LFO pulse
                val bassLfo = 0.6 + 0.4 * sin(2.0 * PI * 0.25 * t)
                val subBass = 0.45 * sin(2.0 * PI * 55.0 * t) * bassLfo

                // Synth Arp lead wave (Sawtooth + Sine harmonic)
                val noteTime = t % eighthNote
                val env = Math.exp(-6.0 * noteTime) // Percussive synth decay envelope
                val synthLead = 0.35 * env * (0.6 * sin(2.0 * PI * targetFreq * t) + 0.4 * sawWave(targetFreq, t))

                // High cyber pad background swell (330Hz E4)
                val padSwell = 0.15 * sin(2.0 * PI * 330.0 * t) * (0.5 + 0.5 * sin(2.0 * PI * 0.1 * t))

                subBass + synthLead + padSwell
            }

            CyberBgmTrack.CYBER_AMBINDEX -> {
                // Dark Matrix Ambient Drone
                val subBass = 0.5 * sin(2.0 * PI * 43.65 * t) // F0 Sub
                val binauralPulse = 0.35 * sin(2.0 * PI * 110.0 * t) * sin(2.0 * PI * 113.5 * t) // Binaural beat effect
                val glitchResonance = 0.15 * sin(2.0 * PI * 440.0 * t) * (0.5 + 0.5 * sin(2.0 * PI * 2.0 * t))
                val deepPad = 0.2 * sawWave(87.31, t) * (0.5 + 0.5 * sin(2.0 * PI * 0.05 * t))

                subBass + binauralPulse + glitchResonance + deepPad
            }

            CyberBgmTrack.RETRO_CYBERDECK -> {
                // Retro Cyberdeck 128 BPM Beat
                val beatDuration = 60.0 / 128.0
                val sixteenthNote = beatDuration / 4.0
                val step = ((t / sixteenthNote).toInt()) % 16

                // Kick on 0, 4, 8, 12
                val isKick = (step % 4 == 0)
                val stepTime = t % sixteenthNote
                val kickEnv = if (isKick) Math.exp(-25.0 * stepTime) else 0.0
                val kickFreq = 120.0 * Math.exp(-30.0 * stepTime) + 45.0
                val kick = 0.6 * kickEnv * sin(2.0 * PI * kickFreq * stepTime)

                // Cyber Synth Lead 16th Arp
                val scale = doubleArrayOf(220.0, 261.63, 329.63, 392.0, 440.0, 523.25, 440.0, 392.0)
                val freq = scale[step % scale.size]
                val synthEnv = Math.exp(-12.0 * stepTime)
                val lead = 0.3 * synthEnv * sawWave(freq, t)

                // Low Sub Bassline (A1 55Hz / F1 43.65Hz)
                val bassFreq = if (step < 8) 55.0 else 43.65
                val bass = 0.35 * sin(2.0 * PI * bassFreq * t)

                kick + lead + bass
            }
        }
    }

    private fun sawWave(freq: Double, t: Double): Double {
        val phase = (t * freq) % 1.0
        return 2.0 * phase - 1.0
    }
}

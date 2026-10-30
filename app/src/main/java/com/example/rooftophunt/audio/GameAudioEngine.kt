package com.example.rooftophunt.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class GameAudioEngine {
    private val sampleRate = 22050
    var soundEnabled: Boolean = true
    var musicEnabled: Boolean = true
    var masterVolume: Float = 0.8f

    private var musicJob: Job? = null
    private val audioScope = CoroutineScope(Dispatchers.Default)

    fun playSound(name: String) {
        if (!soundEnabled) return
        audioScope.launch {
            try {
                when (name) {
                    "coin_pickup" -> playCoin()
                    "jump" -> playJump()
                    "land" -> playThud()
                    "speed_boost" -> playSpeedWhoosh()
                    "shield_activate" -> playShieldHum()
                    "heal_chime" -> playHealChord()
                    "hazard_hit" -> playNoiseCrunch()
                    "capture_horn" -> playCaptureAlarm()
                    "victory_fanfare" -> playVictoryFanfare()
                    "start_whistle" -> playStartWhistle()
                }
            } catch (_: Exception) {}
        }
    }

    fun startMusic() {
        if (!musicEnabled || musicJob?.isActive == true) return
        musicJob = audioScope.launch {
            // Cyberpunk synth bass rhythm generator
            val bassFreqs = floatArrayOf(65.4f, 65.4f, 77.8f, 65.4f, 87.3f, 77.8f, 65.4f, 98.0f)
            var noteIdx = 0

            while (isActive && musicEnabled) {
                val f = bassFreqs[noteIdx % bassFreqs.size]
                playSynthTone(f, 0.18f, 0.22f * masterVolume)
                delay(200)
                noteIdx++
            }
        }
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    private fun playTone(freq: Float, durationSec: Float, vol: Float = 0.5f) {
        val numSamples = (durationSec * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        val adjustedVol = vol * masterVolume

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val sample = sin(2.0 * Math.PI * freq * t) * (1.0 - t / durationSec)
            buffer[i] = (sample * 32767 * adjustedVol).toInt().toShort()
        }
        writeAndPlay(buffer)
    }

    private fun playSynthTone(freq: Float, durationSec: Float, vol: Float) {
        val numSamples = (durationSec * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            // Square + Sawtooth synth wave
            val s1 = sin(2.0 * Math.PI * freq * t)
            val s2 = sin(4.0 * Math.PI * freq * t) * 0.5
            val sample = (s1 + s2) * (1.0 - t / durationSec)
            buffer[i] = (sample * 32767 * vol).toInt().toShort()
        }
        writeAndPlay(buffer)
    }

    private fun playCoin() {
        // Bright 2-tone arpeggio (B5 -> E6)
        playTone(987.77f, 0.08f, 0.6f)
        playTone(1318.51f, 0.14f, 0.7f)
    }

    private fun playJump() {
        val numSamples = (0.12f * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val freq = 200f + t * 2500f // Pitch sweeps up
            val sample = sin(2.0 * Math.PI * freq * t) * (1.0 - t / 0.12f)
            buffer[i] = (sample * 32767 * 0.5f * masterVolume).toInt().toShort()
        }
        writeAndPlay(buffer)
    }

    private fun playThud() {
        playTone(70f, 0.15f, 0.7f)
    }

    private fun playSpeedWhoosh() {
        val numSamples = (0.25f * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val freq = 300f + t * 1800f
            val sample = sin(2.0 * Math.PI * freq * t) * (1.0 - t / 0.25f)
            buffer[i] = (sample * 32767 * 0.6f * masterVolume).toInt().toShort()
        }
        writeAndPlay(buffer)
    }

    private fun playShieldHum() {
        playTone(140f, 0.35f, 0.7f)
    }

    private fun playHealChord() {
        playTone(523.25f, 0.15f, 0.5f) // C5
        playTone(659.25f, 0.15f, 0.5f) // E5
        playTone(783.99f, 0.25f, 0.6f) // G5
    }

    private fun playNoiseCrunch() {
        val numSamples = (0.2f * sampleRate).toInt()
        val buffer = ShortArray(numSamples)
        var rnd = 1234567
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            rnd = (rnd * 1103515245 + 12345) and 0x7FFFFFFF
            val noise = ((rnd % 2000) - 1000) / 1000.0
            val decay = (1.0 - t / 0.2f)
            buffer[i] = (noise * decay * 32767 * 0.7f * masterVolume).toInt().toShort()
        }
        writeAndPlay(buffer)
    }

    private fun playCaptureAlarm() {
        playTone(880f, 0.15f, 0.8f)
        playTone(440f, 0.2f, 0.8f)
        playTone(880f, 0.25f, 0.9f)
    }

    private fun playVictoryFanfare() {
        playTone(523.25f, 0.12f, 0.7f) // C5
        playTone(659.25f, 0.12f, 0.7f) // E5
        playTone(783.99f, 0.12f, 0.7f) // G5
        playTone(1046.50f, 0.35f, 0.8f) // C6
    }

    private fun playStartWhistle() {
        playTone(1200f, 0.25f, 0.8f)
    }

    private fun writeAndPlay(buffer: ShortArray) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
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
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()
    }
}

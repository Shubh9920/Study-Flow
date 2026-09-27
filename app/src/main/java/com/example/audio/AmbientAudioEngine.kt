package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class AmbientSoundType(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val defaultVolume: Float
) {
    RAIN("rain", "Gentle Rain", "Soothing rhythmic rainfall & gusts", "🌧️", 0.7f),
    BROWN_NOISE("brown_noise", "Deep Brown Noise", "Warm low-frequency drone for flow", "🤎", 0.6f),
    WHITE_NOISE("white_noise", "Pure White Noise", "Full-spectrum crisp focus mask", "💨", 0.5f),
    CAFE("cafe", "Cozy Coffeehouse", "Mellow acoustic cafe atmosphere", "☕", 0.6f),
    FOREST("forest", "Forest Breeze", "Rustling leaves & distant chirps", "🌲", 0.65f),
    OCEAN("ocean", "Ocean Waves", "Rhythmic rolling surf & tidal swell", "🌊", 0.7f),
    BINAURAL("binaural", "Alpha Flow (10Hz)", "Binaural frequency for peak study", "🎧", 0.55f),
    CAMPFIRE("campfire", "Night Campfire", "Soft embers & crackling wood", "🔥", 0.65f)
}

class AmbientAudioEngine {
    private val sampleRate = 22050
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(sampleRate / 4)

    private var audioTrack: AudioTrack? = null
    private var isEngineRunning = false
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Active track volumes: sound id -> volume (0.0 to 1.0)
    private val activeTrackVolumes = ConcurrentHashMap<String, Float>()
    private var masterVolume: Float = 0.8f

    fun isPlaying(type: AmbientSoundType): Boolean {
        return activeTrackVolumes.containsKey(type.id) && (activeTrackVolumes[type.id] ?: 0f) > 0f
    }

    fun getTrackVolume(type: AmbientSoundType): Float {
        return activeTrackVolumes[type.id] ?: type.defaultVolume
    }

    fun isAnyPlaying(): Boolean {
        return isEngineRunning && activeTrackVolumes.isNotEmpty()
    }

    fun getMasterVolume(): Float = masterVolume

    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
    }

    fun toggleTrack(type: AmbientSoundType, onStateChanged: (() -> Unit)? = null) {
        if (activeTrackVolumes.containsKey(type.id)) {
            activeTrackVolumes.remove(type.id)
            if (activeTrackVolumes.isEmpty()) {
                stopEngine()
            }
        } else {
            activeTrackVolumes[type.id] = type.defaultVolume
            if (!isEngineRunning) {
                startEngine()
            }
        }
        onStateChanged?.invoke()
    }

    fun setTrackVolume(type: AmbientSoundType, volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (clamped <= 0.01f) {
            activeTrackVolumes.remove(type.id)
            if (activeTrackVolumes.isEmpty()) {
                stopEngine()
            }
        } else {
            activeTrackVolumes[type.id] = clamped
            if (!isEngineRunning) {
                startEngine()
            }
        }
    }

    fun stopAll(onStateChanged: (() -> Unit)? = null) {
        activeTrackVolumes.clear()
        stopEngine()
        onStateChanged?.invoke()
    }

    @Synchronized
    private fun startEngine() {
        if (isEngineRunning) return
        isEngineRunning = true

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()

            playbackJob = scope.launch {
                runSynthesisLoop()
            }
        } catch (_: Exception) {
            isEngineRunning = false
        }
    }

    @Synchronized
    private fun stopEngine() {
        isEngineRunning = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    private suspend fun runSynthesisLoop() {
        val chunk = ShortArray(bufferSize)
        val rnd = Random(System.currentTimeMillis())

        // Synthesis internal states
        var brownLast = 0.0
        var rainPhase = 0.0
        var oceanPhase = 0.0
        var binauralPhase1 = 0.0
        var binauralPhase2 = 0.0
        var forestPhase = 0.0
        var cafePhase = 0.0
        var chirpTimer = 0
        var campfireCrackleCooldown = 0

        while (scope.isActive && isEngineRunning) {
            val track = audioTrack ?: break
            val currentMaster = masterVolume
            val activeSounds = activeTrackVolumes.toMap()

            if (activeSounds.isEmpty() || currentMaster <= 0f) {
                chunk.fill(0)
                track.write(chunk, 0, chunk.size)
                continue
            }

            for (i in chunk.indices) {
                var mixedSample = 0.0

                // 1. Rain
                val rainVol = activeSounds[AmbientSoundType.RAIN.id]
                if (rainVol != null && rainVol > 0f) {
                    rainPhase += 0.0003
                    val windSwell = 0.7 + 0.3 * sin(rainPhase)
                    val rawWhite = (rnd.nextDouble() * 2.0 - 1.0)
                    // Drop impulse
                    val drop = if (rnd.nextDouble() < 0.015) (rnd.nextDouble() * 1.5 - 0.75) else 0.0
                    val rainSample = (rawWhite * 0.4 + drop) * windSwell
                    mixedSample += rainSample * rainVol * 0.45
                }

                // 2. Brown Noise
                val brownVol = activeSounds[AmbientSoundType.BROWN_NOISE.id]
                if (brownVol != null && brownVol > 0f) {
                    val white = rnd.nextDouble() * 2.0 - 1.0
                    brownLast = (brownLast + (0.02 * white)) / 1.02
                    mixedSample += (brownLast * 3.5) * brownVol * 0.45
                }

                // 3. White Noise
                val whiteVol = activeSounds[AmbientSoundType.WHITE_NOISE.id]
                if (whiteVol != null && whiteVol > 0f) {
                    val white = rnd.nextDouble() * 2.0 - 1.0
                    mixedSample += white * whiteVol * 0.2
                }

                // 4. Cafe Murmur
                val cafeVol = activeSounds[AmbientSoundType.CAFE.id]
                if (cafeVol != null && cafeVol > 0f) {
                    cafePhase += 0.0007
                    val drone1 = sin(cafePhase * 180.0) * 0.15
                    val drone2 = sin(cafePhase * 260.0) * 0.1
                    val filteredNoise = (rnd.nextDouble() * 2.0 - 1.0) * 0.25 * (0.8 + 0.2 * sin(cafePhase * 4.0))
                    mixedSample += (drone1 + drone2 + filteredNoise) * cafeVol * 0.5
                }

                // 5. Forest
                val forestVol = activeSounds[AmbientSoundType.FOREST.id]
                if (forestVol != null && forestVol > 0f) {
                    forestPhase += 0.0002
                    val breeze = (rnd.nextDouble() * 2.0 - 1.0) * (0.15 + 0.1 * sin(forestPhase * 6.0))
                    var birdChirp = 0.0
                    if (chirpTimer <= 0) {
                        if (rnd.nextDouble() < 0.00015) {
                            chirpTimer = (sampleRate * 0.12).toInt()
                        }
                    } else {
                        birdChirp = sin(chirpTimer * 0.8) * 0.3
                        chirpTimer--
                    }
                    mixedSample += (breeze + birdChirp) * forestVol * 0.5
                }

                // 6. Ocean Waves
                val oceanVol = activeSounds[AmbientSoundType.OCEAN.id]
                if (oceanVol != null && oceanVol > 0f) {
                    oceanPhase += (2.0 * PI) / (sampleRate * 7.5) // ~7.5s cycle
                    val swell = (sin(oceanPhase).coerceAtLeast(0.0)).let { it * it }
                    val waveWash = (rnd.nextDouble() * 2.0 - 1.0) * (0.05 + 0.55 * swell)
                    mixedSample += waveWash * oceanVol * 0.5
                }

                // 7. Binaural Alpha Beats (220 Hz carrier, 10 Hz difference)
                val binauralVol = activeSounds[AmbientSoundType.BINAURAL.id]
                if (binauralVol != null && binauralVol > 0f) {
                    binauralPhase1 += (2.0 * PI * 220.0) / sampleRate
                    binauralPhase2 += (2.0 * PI * 230.0) / sampleRate
                    val tone1 = sin(binauralPhase1)
                    val tone2 = sin(binauralPhase2)
                    // The combination generates a 10 Hz acoustic beat
                    mixedSample += ((tone1 + tone2) * 0.5) * binauralVol * 0.25
                }

                // 8. Campfire Crackle
                val campfireVol = activeSounds[AmbientSoundType.CAMPFIRE.id]
                if (campfireVol != null && campfireVol > 0f) {
                    val lowHiss = (rnd.nextDouble() * 2.0 - 1.0) * 0.1
                    var pop = 0.0
                    if (campfireCrackleCooldown <= 0) {
                        if (rnd.nextDouble() < 0.0006) {
                            pop = (rnd.nextDouble() * 2.0 - 1.0) * 0.8
                            campfireCrackleCooldown = (sampleRate * 0.02).toInt()
                        }
                    } else {
                        campfireCrackleCooldown--
                    }
                    mixedSample += (lowHiss + pop) * campfireVol * 0.5
                }

                val finalSample = (mixedSample * currentMaster).coerceIn(-1.0, 1.0)
                chunk[i] = (finalSample * Short.MAX_VALUE).toInt().toShort()
            }

            track.write(chunk, 0, chunk.size)
        }
    }
}

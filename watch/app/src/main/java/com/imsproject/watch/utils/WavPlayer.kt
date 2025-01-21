package com.imsproject.watch.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.VolumeShaper
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class WavPlayer(private val context: Context) {

    private val tracks = Array<AudioTrack?>(32) { null }
    private val wavs = Array<WavFile?>(32) { null }
    private val jobs = Array<Job?>(32) { null }
    val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Load a wav file into a track
     *
     * the wav file must have the following parameters:
     * - 16 bit PCM
     * - 44100 Hz sample rate
     * - no compression
     * - stereo
     *
     * @throws IllegalArgumentException if the wav file is incompatible
     */
    @Throws (IllegalArgumentException::class)
    fun load(@IntRange(0, 31) trackNumber: Int, resId: Int) {
        val resourceName = context.resources.getResourceName(resId).split("raw/").last()
        val wav: WavFile
        try{
            wav = WavFile.parse(context.resources.openRawResource(resId).readBytes())
        } catch (e: WavFile.IncompatibleWavFileException){
            throw IllegalArgumentException("Incompatible wav file '$resourceName': ${e.message}")
        }
        wavs[trackNumber] = wav
        tracks[trackNumber] = createAudioTrack(wav)
    }

    /**
     * Play a track
     *
     * onFinished is called iff the track finishes playing normally.
     * It is not called if the track is stopped.
     */
    fun play(@IntRange(0,31) trackNumber: Int, onFinished: () -> Unit = {}) {
        val duration = getDuration(trackNumber)
        val track = tracks[trackNumber] ?: throw IllegalArgumentException("Track not loaded")
        track.play()
        jobs[trackNumber]?.cancel() // cancel previous job if it exists
        jobs[trackNumber] = scope.launch {
            delay((duration * 1000.0).toLong())
            onFinished()
            track.stop() // stop the track so it can be played again
        }
    }

    fun stop(@IntRange(0,31) trackNumber: Int) {
        tracks[trackNumber]?.stop() ?: throw IllegalArgumentException("Track not loaded")
        jobs[trackNumber]?.cancel()
    }

    fun stopFadeOut(@IntRange(0,31) trackNumber: Int, fadeDuration: Long) {
        val track = tracks[trackNumber] ?: throw IllegalArgumentException("Track not loaded")
        val fadeOutConfig = VolumeShaper.Configuration.Builder()
            .setInterpolatorType(VolumeShaper.Configuration.INTERPOLATOR_TYPE_LINEAR)
            .setDuration(fadeDuration)
            .setCurve(floatArrayOf(0f, 1f), floatArrayOf(1f, 0f))
            .build()
        val volumeShaper = track.createVolumeShaper(fadeOutConfig)
        jobs[trackNumber]?.cancel()
        scope.launch {
            volumeShaper.apply(VolumeShaper.Operation.PLAY)
            delay(fadeDuration)
            track.stop()
            volumeShaper.close()
        }
    }

    fun playLooped(@IntRange(0,31) trackNumber: Int) {
        val track = tracks[trackNumber] ?: throw IllegalArgumentException("Track not loaded")
        track.setLoopPoints(0, track.bufferSizeInFrames, -1)
        track.play()
    }

    fun pause(@IntRange(0,31) trackNumber: Int) {
        tracks[trackNumber]?.pause() ?: throw IllegalArgumentException("Track not loaded")
    }

    fun release(@IntRange(0,31) trackNumber: Int) {
        tracks[trackNumber]?.release() ?: throw IllegalArgumentException("Track not loaded")
        tracks[trackNumber] = null
        wavs[trackNumber] = null
    }

    fun releaseAll() {
        for (i in 0 until 32) {
            if(tracks[i] != null){
                release(i)
            }
        }
    }

    fun setVolume(
        @IntRange(0, 31) trackNumber: Int,
        @FloatRange(from = 0.0, to = 1.0) volume: Float
    ) {
        tracks[trackNumber]?.setVolume(volume) ?: throw IllegalArgumentException("Track not loaded")
    }

    fun isPlaying(@IntRange(0,31) trackNumber: Int) : Boolean {
        return tracks[trackNumber]?.playState == AudioTrack.PLAYSTATE_PLAYING
    }

    fun getDuration(@IntRange(0,31) trackNumber: Int) : Double {
        val wav = wavs[trackNumber] ?: throw IllegalArgumentException("Track not loaded")
        return wav.dataSize.toDouble() / wav.byteRate.toDouble()
    }

    private fun createAudioTrack(wav: WavFile) : AudioTrack {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        var audioTrack: AudioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(wav.dataSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
        audioTrack.write(wav.audioData, 0, wav.dataSize)
        return audioTrack
    }
}
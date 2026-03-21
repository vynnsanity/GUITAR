package com.thankgod.guitartutor

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles real-time audio analysis using TarsosDSP.
 */
class AudioProcessor(
    private val onPitchDetected: (Float, Float) -> Unit
) {
    private var dispatcher: AudioDispatcher? = null
    private val sampleRate = 44100
    private val bufferSize = 4096
    private val overlap = 0

    fun start() {
        stop() // Ensure any previous instance is stopped

        // Create a PitchDetectionHandler that will be called for every processed buffer
        val pdh = PitchDetectionHandler { result, _ ->
            val pitchInHz = result.pitch
            val probability = result.probability
            
            // Invoke callback on Main thread for UI updates
            CoroutineScope(Dispatchers.Main).launch {
                onPitchDetected(pitchInHz, probability)
            }
        }

        // Initialize PitchProcessor with FFT_YIN algorithm as requested
        val pitchProcessor = PitchProcessor(
            PitchEstimationAlgorithm.FFT_YIN,
            sampleRate.toFloat(),
            bufferSize,
            pdh
        )

        // Create dispatcher from default microphone
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap)
        dispatcher?.addAudioProcessor(pitchProcessor)

        // Run dispatcher in a background thread
        Thread(dispatcher, "Audio Dispatcher").start()
    }

    fun stop() {
        dispatcher?.stop()
        dispatcher = null
    }
}

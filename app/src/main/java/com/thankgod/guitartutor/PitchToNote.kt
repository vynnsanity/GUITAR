package com.thankgod.guitartutor

import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * Utility to convert frequency in Hz to musical notes.
 */
object PitchToNote {
    private val NOTES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /**
     * Returns the note name (e.g., "A", "C#") for a given frequency.
     */
    fun getNoteName(frequency: Float): String {
        if (frequency <= 0) return "--"
        // Calculate the number of semitones from A4 (440Hz)
        val n = 12 * log2(frequency / 440.0) + 69
        val noteIndex = n.roundToInt() % 12
        return if (noteIndex >= 0) NOTES[noteIndex] else NOTES[noteIndex + 12]
    }

    /**
     * Returns the note name with its octave (e.g., "E2", "A4").
     */
    fun getNoteWithOctave(frequency: Float): String {
        if (frequency <= 0) return "--"
        val n = 12 * log2(frequency / 440.0) + 69
        val noteIndex = n.roundToInt()
        val octave = (noteIndex / 12) - 1
        val name = NOTES[noteIndex % 12]
        return "$name$octave"
    }

    /**
     * Standard guitar string frequencies.
     */
    val guitarStrings = mapOf(
        "E2" to 82.41f,
        "A2" to 110.00f,
        "D3" to 146.83f,
        "G3" to 196.00f,
        "B3" to 246.94f,
        "E4" to 329.63f
    )
}

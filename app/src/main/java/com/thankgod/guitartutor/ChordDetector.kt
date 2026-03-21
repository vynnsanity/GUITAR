package com.thankgod.guitartutor

/**
 * Logic to infer guitar chords from a set of detected notes.
 */
class ChordDetector {

    // Simple map of notes to Chord names
    // For a more robust implementation, one would use intervals or a more complex music theory engine.
    private val chordMap = mapOf(
        setOf("C", "E", "G") to "C Major",
        setOf("A", "C", "E") to "A Minor",
        setOf("G", "B", "D") to "G Major",
        setOf("D", "F#", "A") to "D Major",
        setOf("E", "G#", "B") to "E Major",
        setOf("E", "G", "B") to "E Minor",
        setOf("F", "A", "C") to "F Major",
        setOf("A", "C#", "E") to "A Major",
        setOf("B", "D#", "F#") to "B Major",
        setOf("B", "D", "F#") to "B Minor"
    )

    private val noteBuffer = mutableSetOf<String>()
    private var lastNoteTime = 0L
    private val WINDOW_MS = 500L

    /**
     * Adds a note to the buffer and tries to detect a chord.
     * Returns the chord name if detected, null otherwise.
     */
    fun processNote(note: String): String? {
        val currentTime = System.currentTimeMillis()
        
        // If too much time has passed since the last note, clear the buffer
        if (currentTime - lastNoteTime > WINDOW_MS) {
            noteBuffer.clear()
        }
        
        if (note != "--") {
            noteBuffer.add(note)
            lastNoteTime = currentTime
        }

        // Try to find a match in the chord map
        // We look for a chord that contains all notes in our buffer
        for ((notes, chordName) in chordMap) {
            if (noteBuffer.size >= 3 && noteBuffer.all { it in notes } && notes.size == noteBuffer.size) {
                return chordName
            }
        }

        return null
    }

    fun clear() {
        noteBuffer.clear()
    }
}

package com.learnarm.core.audio

import java.io.File

interface SpeechRecorder {

    /**
     * Begin capturing microphone audio to a new temporary file. Caller MUST
     * have RECORD_AUDIO permission already granted.
     *
     * @return true if recording started, false if a previous recording was
     *   still active or the recorder failed to initialise.
     */
    fun start(): Boolean

    /**
     * Stop the active recording and return the file it was written to, or
     * null if no recording was active. Caller owns the returned file and is
     * responsible for deleting it once it's been uploaded/scored.
     */
    fun stop(): File?

    /** Stop without preserving the file. */
    fun cancel()

    val isRecording: Boolean
}

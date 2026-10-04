package io.github.jamisuni.tangram.settings

// SCAFFOLDING (disposable), written by S-2. decision DA-114: the real AudioTrack output can fail loudly.
import android.content.Context
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class AudioOutSmokeScaffoldingTest {
    private companion object {
        const val TAG = "AudioOutSmoke"

        /** Named waiver: API levels on which a missing output device skips loudly instead of failing.
         *  Empty until the orchestrator adds one with a logged governance row-16 waiver. */
        val NO_AUDIO_OUTPUT_WAIVED_API_LEVELS: Set<Int> = emptySet()

        /** The brief asks for the playback head to be asserted where an output exists (the design says record only). */
        const val ASSERT_PLAYBACK_HEAD = true
    }

    private fun hasOutput(): Boolean {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).isNotEmpty()
    }

    private fun awaitReady(out: AudioTrackSoundOut, ms: Long = 5000) {
        val end = SystemClock.elapsedRealtime() + ms
        while (!out.ready && SystemClock.elapsedRealtime() < end) Thread.sleep(20)
        assertTrue("prepare() not ready within ${ms}ms", out.ready)
    }

    private fun assertAllInitialized(out: AudioTrackSoundOut) {
        val states = out.trackStates()
        assertEquals("track count", FeedbackEvent.entries.size, states.size)
        for (e in FeedbackEvent.entries) {
            assertEquals("track $e state", AudioTrack.STATE_INITIALIZED, states[e])
        }
        assertEquals("buildFailures", 0, out.buildFailures)
    }

    @Test
    fun audioOutputWorks() {
        if (!hasOutput()) {
            val msg = "NO AUDIO OUTPUT DEVICE on API ${Build.VERSION.SDK_INT} (${Build.MODEL})"
            Log.e(TAG, "!!!! $msg")
            System.err.println("!!!! $msg")
            if (Build.VERSION.SDK_INT in NO_AUDIO_OUTPUT_WAIVED_API_LEVELS) {
                assumeTrue("WAIVED (NO_AUDIO_OUTPUT_WAIVED_API_LEVELS): $msg", false)
            }
            throw AssertionError("$msg; no waiver listed")
        }

        val out = AudioTrackSoundOut()
        try {
            out.prepare()
            awaitReady(out)
            assertAllInitialized(out)
            for (e in FeedbackEvent.entries) {
                repeat(2) {
                    out.play(e)
                    Thread.sleep(30)
                }
                assertEquals("replayFailures after $e", 0, out.replayFailures)
                out.play(e)
                val end = SystemClock.elapsedRealtime() + 300
                var head = 0
                while (head <= 0 && SystemClock.elapsedRealtime() < end) {
                    head = out.playbackHeadPosition(e)
                    Thread.sleep(10)
                }
                val line = "RECORD playbackHead $e = $head (output present)"
                Log.i(TAG, line)
                println(line)
                if (ASSERT_PLAYBACK_HEAD) assertTrue("playback head did not advance for $e", head > 0)
                Thread.sleep(SoundSynth.durationMs(e).toLong() + 20)
            }
            assertEquals("replayFailures", 0, out.replayFailures)
        } finally {
            out.release()
        }

        repeat(20) {
            val o = AudioTrackSoundOut()
            o.prepare()
            awaitReady(o)
            assertAllInitialized(o)
            o.release()
        }

        repeat(20) {
            val o = AudioTrackSoundOut()
            o.prepare()
            o.release()
        }
        Thread.sleep(1500) // let any straggling build threads finish and release

        val fresh = AudioTrackSoundOut()
        try {
            fresh.prepare()
            awaitReady(fresh)
            assertAllInitialized(fresh)
            fresh.play(FeedbackEvent.LOCK)
            assertEquals("fresh replayFailures", 0, fresh.replayFailures)
        } finally {
            fresh.release()
        }
    }
}

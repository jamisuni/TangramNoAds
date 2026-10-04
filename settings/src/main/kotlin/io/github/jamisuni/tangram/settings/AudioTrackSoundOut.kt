package io.github.jamisuni.tangram.settings

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The real sound output (decision DA-114, design WO-007 3.3): five synthesized cues, one MODE_STATIC
 * AudioTrack each, built once on one background thread and published through one volatile immutable map.
 * No audio focus, no permission, no asset. `play` never throws and drops silently before ready.
 */
class AudioTrackSoundOut : SoundOut {
    @Volatile private var tracks: Map<FeedbackEvent, AudioTrack> = emptyMap()
    @Volatile private var released = false
    @Volatile private var published = false
    @Volatile private var builds = 0
    @Volatile private var replays = 0
    private val started = AtomicBoolean(false)

    /** True once the build thread has published at least one track (and the instance is not released). */
    internal val ready: Boolean get() = published && !released && tracks.isNotEmpty()
    internal val buildFailures: Int get() = builds
    internal val replayFailures: Int get() = replays

    internal fun trackStates(): Map<FeedbackEvent, Int> =
        tracks.mapValues { (_, t) -> runCatching { t.state }.getOrDefault(AudioTrack.STATE_UNINITIALIZED) }

    /** Smoke-test probe: the playback head of one cue's track, or -1. */
    internal fun playbackHeadPosition(event: FeedbackEvent): Int =
        runCatching { tracks[event]?.playbackHeadPosition }.getOrNull() ?: -1

    /** Idempotent; starts one background thread. A no-op after [release]. */
    fun prepare() {
        if (released || !started.compareAndSet(false, true)) return
        Thread({ build() }, "tangram-audio-build").apply { isDaemon = true }.start()
    }

    private fun build() {
        val built = LinkedHashMap<FeedbackEvent, AudioTrack>()
        try {
            val rate = nativeRate()
            for (event in FeedbackEvent.entries) {
                if (released) break // checked before each build
                val t = runCatching { buildTrack(SoundSynth.pcm(event, rate), rate) }.getOrNull()
                if (t == null) builds++ else built[event] = t
            }
            if (released) { // checked after the last build
                built.values.forEach(::safeRelease)
                return
            }
            tracks = Collections.unmodifiableMap(LinkedHashMap(built)) // one assignment: all or none
            published = true
            if (released) { // and again after publishing: release() may have swapped before this write
                val mine = tracks
                tracks = emptyMap()
                mine.values.forEach(::safeRelease)
            }
        } catch (t: Throwable) {
            builds++
            built.values.forEach(::safeRelease)
        }
    }

    private fun nativeRate(): Int {
        val r = runCatching { AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC) }.getOrDefault(0)
        return if (r > 0) r else FALLBACK_RATE
    }

    private fun buildTrack(pcm: ShortArray, rate: Int): AudioTrack? {
        val bytes = pcm.size * 2
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY) // a hint only
            .build()
        val written = t.write(pcm, 0, pcm.size)
        if (written < 0) {
            safeRelease(t)
            return null
        }
        return t
    }

    override fun play(event: FeedbackEvent) {
        try {
            val t = tracks[event] ?: return
            if (t.state != AudioTrack.STATE_INITIALIZED) return
            t.stop()
            if (t.reloadStaticData() != AudioTrack.SUCCESS) {
                replays++
                return
            }
            t.play()
        } catch (e: Throwable) {
            replays++ // a thrown stop()/play() is counted, never swallowed silently, never rethrown
        }
    }

    /** Safe at any time, also before [prepare] has finished; idempotent. */
    fun release() {
        released = true
        val old = tracks
        tracks = emptyMap()
        old.values.forEach(::safeRelease)
    }

    private fun safeRelease(t: AudioTrack) {
        runCatching { t.release() }
    }

    private companion object {
        const val FALLBACK_RATE = 48_000
    }
}

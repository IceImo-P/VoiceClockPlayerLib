package net.imoya.android.voiceclock.player.thread

import android.content.Context
import net.imoya.android.voiceclock.player.PlayerUtils.convertResourcesToRawAudioTasks
import net.imoya.android.media.audio.raw.RawAudioSequenceItem
import net.imoya.android.media.audio.raw.RawAudioSequencer
import net.imoya.android.media.audio.resource.ResourceAudioSequenceItem
import net.imoya.android.util.Log
import net.imoya.android.voiceclock.player.property.VoiceClockPlayerProperty
import java.lang.Exception
import java.util.concurrent.TimeUnit
import kotlin.concurrent.withLock

/**
 * 音声を連続再生するスレッドです。(Version 2)
 *
 * Android Eclair (2.1) 以降でデフォルトの再生ロジックです。
 * ※現在は Android 4.0 以降の API を使用しています。
 *
 * 指定されたリソースを先に PCM へデコードして 1個の音声へ結合し、
 * [android.media.AudioTrack] を使用して再生します。
 *
 * 再生開始までの delay が長く重いですが、機種ごとの差異が少なく比較的安定しています。
 * ただし一部の機種(古い Galaxy 等)では、
 * [android.media.AudioTrack] に互換性が無くノイズが再生される問題があります。
 *
 * @param context [Context]
 * @param property [VoiceClockPlayerProperty]
 * @param tasks 連続再生する音声リソースの指定
 * @param playerId 複数のインスタンスを識別するためのID
 *
 * @author IceImo-P
 */
class VoiceClockPlayerThreadV2(
    context: Context,
    private val property: VoiceClockPlayerProperty,
    tasks: Array<ResourceAudioSequenceItem>,
    @Suppress("weaker")
    val playerId: Int
) : VoiceClockPlayerThread(context, tasks) {
    private lateinit var sequencer: RawAudioSequencer
    private val delayBeforeRepeat: Int = property.getVoiceDelayValues()[3]
    private var waitToPlay = true

    override fun play() {
        lock.withLock {
            waitToPlay = false
            condition.signalAll()
        }
    }

    override fun run() {
        Log.v(TAG) { "start. playerId = $playerId" }
        do {
            try {
                // タスクリストのオーディオを Linear PCM へ変換する
                val rawAudioItems = convertResourcesToRawAudioTasks(ct, tasks)

                // Linear PCM へ変換したタスクリストを RawAudioSequencer へ投入する
                setupSequencer(rawAudioItems)

                // AudioTrack をセットアップし、すぐに再生可能な状態にする
                sequencer.prepare()

                // EventListener へ準備完了を通知する
                try {
                    listener?.onPrepare(this)
                } catch (tr: Throwable) {
                    Log.w(TAG, "ERROR at listener#onPrepare", tr)
                }

                lock.withLock {

                    // 再生開始の指示があるまで待つ
                    while (waitToPlay && !disposing) {
                        try {
                            condition.await()
                        } catch (ex: InterruptedException) {
                            Log.d(TAG, ex)
                        }
                    }
                }
                if (disposing) {
                    break
                }

                // 再生する
                do {
                    sequencer.play()
                    if (repeat && !disposing) {
                        // 繰り返しの時は、繰り返す前に少し待つ
                        lock.withLock {
                            try {
                                condition.await(delayBeforeRepeat.toLong(), TimeUnit.MILLISECONDS)
                            } catch (ex: InterruptedException) {
                                Log.d(TAG, ex)
                            }
                        }
                    }
                } while (repeat && !disposing)

                // 中断でなければ、EventListener#onCompletion をコールする
                if (!disposing) {
                    Log.v(TAG, "onCompletion")
                    val l = listener
                    if (l != null) {
                        try {
                            l.onCompletion()
                        } catch (ex: Exception) {
                            Log.e(TAG, "ERROR at EventListener#onCompletion", ex)
                        }
                    }
                }
            } catch (tr: Throwable) {
                Log.w(TAG, tr)

                // 例外発生時は、EventListener#onError をコールする
                val l = listener
                if (l != null) {
                    try {
                        l.onError()
                    } catch (tr2: Throwable) {
                        Log.e(TAG, "ERROR at EventListener#onError", tr2)
                    }
                }
            }
        } while (false)
    }

    /**
     * [sequencer] を初期化します。
     *
     * @param tasks 連続再生するオーディオデータのリスト
     */
    private fun setupSequencer(tasks: Array<RawAudioSequenceItem>) {
        val defaultDelay = property.getVoiceDelayV2()
        sequencer = RawAudioSequencer()
        sequencer.audioUsage = audioUsage
        sequencer.contentType = contentType
        sequencer.playerType = RawAudioSequencer.PlayerType.AUDIO_TRACK
        for (task in tasks) {
            val item = RawAudioSequenceItem(task.delayMilliSeconds + defaultDelay, task.rawAudio)
            sequencer.addSequence(item)
        }
    }

    override fun release() {
        super.release()

        if (this::sequencer.isInitialized) {
            try {
                sequencer.release()
            } catch (e: Exception) {
                Log.v(TAG, "release: Exception at sequencer.release", e)
            }
        }
    }

    companion object {
        /**
         * Tag for log
         */
        private const val TAG = "PlayerThreadV2"
    }
}
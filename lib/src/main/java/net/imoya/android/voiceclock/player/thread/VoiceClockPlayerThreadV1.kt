package net.imoya.android.voiceclock.player.thread

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import net.imoya.android.media.audio.AudioSequenceItem
import net.imoya.android.media.audio.resource.ResourceAudioSequenceItem
import net.imoya.android.util.Log
import net.imoya.android.voiceclock.player.VoiceClockPlayer
import net.imoya.android.voiceclock.player.property.VoiceClockPlayerProperty
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.withLock

/**
 * 音声を連続再生するスレッドです。(Version 1)
 *
 * Android Donut (1.6) までデフォルトだった再生ロジックです。
 *
 * 連続する音声リソースを1個ずつ [MediaPlayer] で再生します。
 * 再生開始までの delay が少なく軽いのですが、機種によって [MediaPlayer] の挙動が異なるため安定しません。
 *
 * @param context [Context]
 * @param property [VoiceClockPlayerProperty]
 * @param tasks 連続再生する音声リソースの指定
 *
 * @author IceImo-P
 */
class VoiceClockPlayerThreadV1(
    context: Context,
    property: VoiceClockPlayerProperty,
    tasks: Array<ResourceAudioSequenceItem>
) : VoiceClockPlayerThread(context, tasks), OnCompletionListener, MediaPlayer.OnErrorListener {
    private lateinit var players: Array<MediaPlayer>
    private var waitToPlay = true
    private var completeTask = false
    private val delayBeforeRepeat: Int

    init {
        delayBeforeRepeat = property.getVoiceDelayValues()[3]
    }

    /**
     * 内部の準備が整い次第、音声の再生を開始します。
     */
    override fun play() {
        lock.withLock {
            waitToPlay = false
            condition.signalAll()
        }
    }

    override fun run() {
        Log.v(TAG, "start")
        try {
            // MediaPlayer を準備する
            players = tasks.map { setupPlayer(it.resourceId) }.toTypedArray()

            // EventListener へ準備完了を通知する
            try {
                listener?.onPrepare(this)
            } catch (tr: Throwable) {
                Log.e(TAG, "ERROR at listener#onPrepare", tr)
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

            if (!disposing) {
                do {
                    playSound()
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
            }

            // 別のスレッドでMediaPlayerが再セットアップされた時に、
            // 現在再生終了を待っているMediaPlayerが停止されるのを防ぐ
            val currentPlayers = players
            players = arrayOf()

            // 中断でなければ、EventListener#onCompletion を呼び出し、
            // MediaPlayer を解放する前にしばらく待つ
            if (!disposing) {
                Log.v(TAG, "onCompletion")

                // EventListener#onCompletion をコールする
                val l = listener
                if (l != null) {
                    try {
                        l.onCompletion()
                    } catch (ex: Exception) {
                        Log.e(TAG, "ERROR at EventListener#onCompletion", ex)
                    }
                }

                // しばらく待つ
                lock.withLock {
                    try {
                        condition.await(VoiceClockPlayer.PLAYER_END_DELAY.toLong(), TimeUnit.MILLISECONDS)
                    } catch (ex: InterruptedException) {
                        Log.d(TAG, ex)
                    }
                }
            }

            // MediaPlayer を解放する
            for (player in currentPlayers) {
                try {
                    player.release()
                } catch (ex: Exception) {
                    Log.d(TAG, "Failed to mp.release", ex)
                }
            }
            Log.v(TAG, "player is released")
        } catch (tr: Throwable) {
            Log.w(TAG, tr)
        }
        Log.v(TAG, "end")
    }

    /**
     * 音声を連続再生します。
     */
    private fun playSound() {
        for (i in tasks.indices) {
            completeTask = false
            val t: AudioSequenceItem = tasks[i]
            val mp = players[i]
            if (i > 0) {
                Log.v(TAG) { "[$i]onContinue" }
                val l = listener
                if (l != null) {
                    try {
                        l.onContinue()
                    } catch (ex: Exception) {
                        Log.e(TAG, "ERROR at EventListener#onContinue", ex)
                    }
                }
            }

            // ディレイが指定されていたら、指定時間だけ待つ。
            if (t.delayMilliSeconds > 0) {
                Log.v(TAG) { "[$i]delay = ${t.delayMilliSeconds}" }
                lock.withLock {
                    try {
                        condition.await(t.delayMilliSeconds.toLong(), TimeUnit.MILLISECONDS)
                    } catch (ex: InterruptedException) {
                        Log.d(TAG, ex)
                    }
                }
                Log.v(TAG) { "[$i]end delay" }
            }

            if (disposing) break

            lock.withLock {
                // 再生開始
                mp.start()

                // 再生終了か中断まで待つ
                Log.d(TAG) { "[$i]waiting for player..." }

                val startTime = System.currentTimeMillis()
                while (!(disposing || completeTask)) {
                    try {
                        condition.await(10000, TimeUnit.MILLISECONDS)

                        // 約10秒待っても終了していなかったら、おそらく修復不能なエラーが
                        // 発生しているので強制終了する
                        // ※同時発音数不足などで再生できない場合、
                        // MediaPlayerは再生していないがコールバックが呼び出されないため、
                        // 状態を検出できずどうしようもなくなる
                        val elapsed = System.currentTimeMillis() - startTime
                        Log.d(TAG) { "[$i]elapsed = $elapsed" }
                        if (elapsed > 9900) {
                            Log.w(TAG) { "[$i]TIMEOUT!" }

                            // EventListener#onError をコールする
                            val l = listener
                            if (l != null) {
                                try {
                                    l.onError()
                                } catch (ex: Exception) {
                                    Log.e(TAG, "ERROR at EventListener#onError", ex)
                                }
                            }
                            disposing = true
                            break
                        } else {
                            Log.v(TAG) { "[$i]end or continue waiting" }
                        }
                    } catch (ex: InterruptedException) {
                        Log.d(TAG, ex)
                    }
                }
                Log.v(TAG) { "[$i]complete" }
            }
            if (disposing) break
        }
    }

    override fun onCompletion(mp: MediaPlayer) {
        if (!disposing) {
            // 待機しているスレッドへ再生終了を通知する
            lock.withLock {
                completeTask = true
                condition.signalAll()
            }
        }
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        Log.v(TAG, "onError: start")
        if (!disposing) {
            // EventListener#onError をコールする
            val l = listener
            if (l != null) {
                try {
                    l.onError()
                } catch (ex: Exception) {
                    Log.e(TAG, "ERROR at EventListener#onError", ex)
                }
            }

            // 待機しているスレッドへ中断を通知する
            lock.withLock {
                disposing = true
                condition.signalAll()
            }
        }
        return true
    }

    /**
     * [MediaPlayer] を初期化します。
     *
     * @param resId 再生する音声リソースのID
     */
    @Throws(IOException::class)
    private fun setupPlayer(resId: Int): MediaPlayer {
        val mp = MediaPlayer()
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(audioUsage)
                .setContentType(contentType)
                .build()
        )
        mp.setDataSource(
            ct, Uri.parse(
                "android.resource://${ct.packageName}/$resId"
            )
        )
        mp.prepare()
        mp.setOnCompletionListener(this)
        return mp
    }

    companion object {
        /**
         * Tag for log
         */
        private const val TAG = "PlayerThreadV1"
    }
}
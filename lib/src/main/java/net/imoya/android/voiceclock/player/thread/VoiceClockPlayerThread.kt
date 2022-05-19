package net.imoya.android.voiceclock.player.thread

import android.content.Context
import android.media.AudioAttributes
import net.imoya.android.media.audio.resource.ResourceAudioSequenceItem
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 音声を連続再生するスレッドの abstract クラス
 *
 * @author IceImo-P
 */
abstract class VoiceClockPlayerThread(
    /**
     * [Context]
     */
    protected val ct: Context,
    /**
     * 連続再生する音声リソース
     */
    protected val tasks: Array<ResourceAudioSequenceItem>
) : Thread() {
    /**
     * イベントリスナー
     */
    interface EventListener {
        /**
         * エラー発生を通知します。
         */
        fun onError()

        /**
         * 準備完了を通知します。
         *
         * @param caller 呼び出し元の [VoiceClockPlayerThread]
         */
        fun onPrepare(caller: VoiceClockPlayerThread)

        /**
         * 音声の繰り返し再生を通知します。
         */
        fun onContinue()

        /**
         * 再生終了を通知します。
         */
        fun onCompletion()
    }

    /**
     * 音声の用途を表す [AudioAttributes].USAGE_* 値
     */
    var audioUsage = AudioAttributes.USAGE_NOTIFICATION_EVENT

    /**
     * 音声の種別を表す [AudioAttributes].CONTENT_TYPE_* 値
     */
    var contentType = AudioAttributes.CONTENT_TYPE_MUSIC

    /**
     * 繰り返し再生フラグ
     */
    var repeat: Boolean = false

    /**
     * [EventListener]
     */
    var listener: EventListener? = null

    /**
     * [release] 実行フラグ。 [release] メソッドをコールすると true となる。
     */
    @JvmField
    protected var disposing = false

    /**
     * スレッド制御用 [ReentrantLock]
     */
    protected val lock: ReentrantLock = ReentrantLock()

    /**
     * スレッド制御用 [Condition]
     */
    protected val condition: Condition = lock.newCondition()

    /**
     * スレッドが保有するリソースを解放し、スレッドを破棄可能な状態にします。
     */
    open fun release() {
        disposing = true
        lock.withLock {
            condition.signalAll()
        }
    }

    /**
     * 内部の準備が整い次第、音声の再生を開始します。
     */
    abstract fun play()
}
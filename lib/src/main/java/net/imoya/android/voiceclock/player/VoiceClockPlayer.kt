package net.imoya.android.voiceclock.player

import android.content.Context
import android.media.AudioAttributes
import net.imoya.android.media.audio.resource.ResourceAudioSequenceItem
import net.imoya.android.util.Log
import net.imoya.android.voiceclock.player.property.VoiceClockEngineVersion
import net.imoya.android.voiceclock.player.property.VoiceClockPlayerProperty
import net.imoya.android.voiceclock.player.property.VoiceClockReadingMode
import net.imoya.android.voiceclock.player.sequence.ResourceSequenceFactory
import net.imoya.android.voiceclock.player.sequence.AmPmSequenceFactory
import net.imoya.android.voiceclock.player.sequence.DefaultSequenceFactory
import net.imoya.android.voiceclock.player.sequence.Hour24SequenceFactory
import net.imoya.android.voiceclock.player.thread.VoiceClockPlayerThread
import net.imoya.android.voiceclock.player.thread.VoiceClockPlayerThreadV1
import net.imoya.android.voiceclock.player.thread.VoiceClockPlayerThreadV2
import java.util.*

/**
 * 時刻読み上げ音声を再生します。
 *
 * @param context  [Context]
 * @param playerId 複数の [VoiceClockPlayer] を識別するID値。
 * 複数の [VoiceClockPlayer] を同時に使用する場合は、それぞれ異なるID値を指定すること。
 * @param property 設定値を提供する [VoiceClockPlayerProperty]
 */
@Suppress("unused")
class VoiceClockPlayer(
    private val context: Context,
    private val playerId: Int,
    private val property: VoiceClockPlayerProperty
) {
    /**
     * 再生時の各種タイミングで呼び出すコールバックを提供します。
     *
     * @author IceImo-P
     */
    interface EventListener {
        fun onContinue(player: VoiceClockPlayer?)
        fun onEnd(player: VoiceClockPlayer?)
        fun onError(player: VoiceClockPlayer?)
    }

    /**
     * 通知音として再生する、音声リソースのID
     *
     * 0 を指定した場合、通知音を再生しない
     */
    private var noticeSoundId = 0

    /**
     * 音声の用途を表す [AudioAttributes].USAGE_* 値
     */
    @Suppress("weaker")
    var audioUsage = AudioAttributes.USAGE_MEDIA

    /**
     * 音声の種別を表す [AudioAttributes].CONTENT_TYPE_* 値
     */
    var contentType = AudioAttributes.CONTENT_TYPE_UNKNOWN

    private var repeat = false
    private var playerThread: VoiceClockPlayerThread? = null

    /**
     * 音声再生中か否かを返します。
     *
     * @return 再生中の場合はtrue, そうでない場合はfalse
     */
    var isPlaying = false
        private set
    private var listener: EventListener? = null

    /**
     * 時刻読み上げ前に鳴らす音を指定します。
     *
     * @param resId 鳴らす音のリソースID, または鳴らさないことを表す0
     */
    fun setNoticeSound(resId: Int) {
        noticeSoundId = resId
    }

    /**
     * 繰り返し再生を行うか否かを設定します。
     *
     * @param enable 繰り返す場合はtrue, そうでない場合はfalse
     */
    fun setRepeat(enable: Boolean) {
        repeat = enable
    }

    /**
     * EventListenerを設定します。
     *
     * @param listener EventListener
     */
    fun setEventListener(listener: EventListener?) {
        this.listener = listener
    }

    /**
     * 音声再生を停止し、リソースを解放します。
     */
    fun release() {
        playerThread?.release()
    }

    /**
     * 現在時刻を読み上げます。
     *
     * @return 再生を開始した場合はtrue, 既に再生中などの理由で再生できなかった場合はfalse
     */
    fun playVoiceCurrent(): Boolean {
        val c = Calendar.getInstance()
        return this.playVoice(
            property.getClockReadingMode(),
            c[Calendar.HOUR_OF_DAY],
            c[Calendar.MINUTE]
        )
    }

    fun playVoiceCurrent(mode: Int): Boolean {
        val c = Calendar.getInstance()
        return this.playVoice(
            mode,
            c[Calendar.HOUR_OF_DAY],
            c[Calendar.MINUTE]
        )
    }

    fun playVoice(hourOfDay: Int, minute: Int): Boolean {
        return this.playVoice(
            property.getClockReadingMode(), hourOfDay, minute
        )
    }

    private fun playVoice(mode: Int, hourOfDay: Int, minute: Int): Boolean {
        val voiceDelay = voiceDelayPreference
        return this.playVoice(
            getTaskListFactory(mode, hourOfDay, minute, voiceDelay), voiceDelay
        )
    }

    private fun getTaskListFactory(
        mode: Int, hourOfDay: Int, minute: Int, voiceDelay: Int
    ): ResourceSequenceFactory {
        Log.v(TAG, "getTaskListFactory: mode = $mode")
        val f: ResourceSequenceFactory = when (mode) {
            VoiceClockReadingMode.MODE_24H -> Hour24SequenceFactory(
                property,
                hourOfDay,
                minute
            )
            VoiceClockReadingMode.MODE_12H_AM_PM -> AmPmSequenceFactory(
                property,
                if (hourOfDay < 12) Calendar.AM else Calendar.PM, hourOfDay, minute
            )
            else -> DefaultSequenceFactory(
                property,
                hourOfDay,
                minute
            )
        }
        f.voiceDelay = voiceDelay
        return f
    }

    @Synchronized
    private fun playVoice(f: ResourceSequenceFactory, voiceDelay: Int): Boolean {
        Log.v(TAG, "playVoice: start")

        // 重複再生はしない
        if (isPlaying) {
            Log.v(TAG, "playVoice: isPlaying")
            return false
        }
        Log.v(TAG, "playVoice: setup players.")

        // 再生する音声を決定する
        val tasks = ArrayList<ResourceAudioSequenceItem>(5)
        if (noticeSoundId != 0) {
            tasks.add(ResourceAudioSequenceItem(0, noticeSoundId))
        }
        try {
            val clockTasks = f.list
            var isFirstClockTask = true
            for (t in clockTasks) {
                if (noticeSoundId != 0 && isFirstClockTask) {
                    isFirstClockTask = false
                    t.delayMilliSeconds += voiceDelay
                }
                tasks.add(t)
            }
        } catch (ex: Exception) {
            Log.e(TAG, { "playVoice: ERROR: Illegal time. ${f.parameters}" }, ex)
            return false
        }

        // 以前に使用した再生スレッドが存在したら、解放する
        playerThread?.release()

        // 再生スレッドをセットアップする。
        val playerThread = setupPlayerThread(tasks.toTypedArray(), playerId)
        playerThread.audioUsage = audioUsage
        playerThread.repeat = repeat
        playerThread.listener = ThreadEventHandler()
        this.playerThread = playerThread

        // 再生を開始する
        isPlaying = true
        playerThread.start()
        return true
    }

    /**
     * エンジンバージョンの設定に応じた、読み上げタイミング設定値を返します。
     *
     * @return 読み上げタイミング設定値
     */
    private val voiceDelayPreference: Int
        get() = when (property.getPlayerEngineVersion()) {
            VoiceClockEngineVersion.V1 -> property.getVoiceDelayV1()
            else -> property.getVoiceDelayV2()
        }

    /**
     * 音声を連続再生するスレッドを初期化して返します。
     *
     * @param tasks    連続再生する音声リソースの指定
     * @param playerId 複数の [VoiceClockPlayer] を識別するID値
     * @return 音声を連続再生するスレッド
     */
    private fun setupPlayerThread(
        tasks: Array<ResourceAudioSequenceItem>,
        playerId: Int
    ): VoiceClockPlayerThread {
        return when (property.getPlayerEngineVersion()) {
            VoiceClockEngineVersion.V2 -> VoiceClockPlayerThreadV2(
                context,
                property,
                tasks,
                playerId
            )
            else -> VoiceClockPlayerThreadV1(
                context,
                property,
                tasks
            )
        }
    }

    private inner class ThreadEventHandler : VoiceClockPlayerThread.EventListener {
        override fun onPrepare(caller: VoiceClockPlayerThread) {
            // 準備ができたら直ちに再生する
            caller.play()
        }

        override fun onCompletion() {
            Log.v(TAG, "onCompletion: start")
            isPlaying = false
            val l = listener
            if (l != null) {
                try {
                    l.onEnd(this@VoiceClockPlayer)
                } catch (ex: Exception) {
                    Log.e(TAG, "ERROR at EventListener#onEnd", ex)
                }
            }
        }

        override fun onContinue() {
            Log.v(TAG, "onContinue: start")
            val l = listener
            if (l != null) {
                try {
                    l.onContinue(this@VoiceClockPlayer)
                } catch (ex: Exception) {
                    Log.e(TAG, "ERROR at EventListener#onContinue", ex)
                }
            }
        }

        override fun onError() {
            Log.v(TAG, "onError: start")
            isPlaying = false
            val l = listener
            if (l != null) {
                try {
                    l.onError(this@VoiceClockPlayer)
                } catch (ex: Exception) {
                    Log.e(TAG, "ERROR at EventListener#onError", ex)
                }
            }
        }
    }

    companion object {
        /**
         * 音声再生終了後に待機する時間(ミリ秒)。
         */
        const val PLAYER_END_DELAY = 250

        /**
         * Tag for log
         */
        private const val TAG = "VoiceClockPlayer"
    }
}
package net.imoya.android.voiceclock.player.property

import net.imoya.android.voiceclock.player.VoiceClockPlayer

/**
 * Provides [VoiceClockPlayer] properties
 */
abstract class VoiceClockPlayerProperty {
    /**
     * 時刻読み上げモードを返します。
     *
     * @return [VoiceClockReadingMode] が定義する値
     */
    abstract fun getClockReadingMode(): Int

    /**
     * 読み上げ音声再生エンジンのバージョンを返します。
     *
     * @return [VoiceClockEngineVersion] が定義する値
     */
    abstract fun getPlayerEngineVersion(): Int

    /**
     * 読み上げエンジン V1 用の読み上げ間隔調整値を返します。
     *
     * @return 読み上げ間隔調整値
     */
    abstract fun getVoiceDelayV1(): Int

    /**
     * 読み上げエンジン V2 用の読み上げ間隔調整値を返します。
     *
     * @return 読み上げ間隔調整値
     */
    abstract fun getVoiceDelayV2(): Int

    /**
     * 音声へ挿入する無音時間の設定値を返します。
     *
     * @return 音声へ挿入する無音時間の設定値
     */
    abstract fun getVoiceDelayValues(): IntArray
}
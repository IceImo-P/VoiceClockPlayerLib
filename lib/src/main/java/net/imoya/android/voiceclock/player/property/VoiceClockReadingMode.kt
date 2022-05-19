package net.imoya.android.voiceclock.player.property

/**
 * 時刻読み上げモード
 */
@Suppress("unused")
object VoiceClockReadingMode {
    /**
     * 12時間(1時 ～ 12時)
     */
    const val MODE_12H = 0

    /**
     * 午前/午後 + 12時間(1時 ～ 12時)
     */
    const val MODE_12H_AM_PM = 1

    /**
     * 24時間 (0時 ～ 23時)
     */
    const val MODE_24H = 2
}
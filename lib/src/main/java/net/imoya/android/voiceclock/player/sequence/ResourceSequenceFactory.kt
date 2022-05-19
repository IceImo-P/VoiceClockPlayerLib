package net.imoya.android.voiceclock.player.sequence

import net.imoya.android.media.audio.resource.ResourceAudioSequenceItem
import net.imoya.android.voiceclock.player.property.VoiceClockPlayerProperty
import net.imoya.android.voiceclock.player.R

/**
 * 再生する音声リソースのリストを作成します。
 *
 * @param property [VoiceClockPlayerProperty]
 */
abstract class ResourceSequenceFactory(@JvmField protected val property: VoiceClockPlayerProperty) {
    protected val defaultDelay: Int
    protected val delayAfterAmPm: Int
    protected val delayAfterHours: Int
    protected val resIdHourJust: IntArray
    protected val resIdHourContinue: IntArray
    protected val resIdMinutes10Just: IntArray
    protected val resIdMinutes10Continue: IntArray
    protected val resIdMinutes1: IntArray
    var voiceDelay = 0

    abstract val list: Array<ResourceAudioSequenceItem>
    abstract val parameters: String

    init {
        val delayValues = property.getVoiceDelayValues()
        defaultDelay = delayValues[0]
        delayAfterAmPm = delayValues[1]
        delayAfterHours = delayValues[2]
        resIdHourJust = intArrayOf(
            R.raw.hn0, R.raw.hn1, R.raw.hn2, R.raw.hn3, R.raw.hn4,
            R.raw.hn5, R.raw.hn6, R.raw.hn7, R.raw.hn8, R.raw.hn9,
            R.raw.hn10, R.raw.hn11, R.raw.hn12, R.raw.hn13, R.raw.hn14,
            R.raw.hn15, R.raw.hn16, R.raw.hn17, R.raw.hn18, R.raw.hn19,
            R.raw.hn20, R.raw.hn21, R.raw.hn22, R.raw.hn23
        )
        resIdHourContinue = intArrayOf(
            R.raw.hc0, R.raw.hc1, R.raw.hc2, R.raw.hc3, R.raw.hc4,
            R.raw.hc5, R.raw.hc6, R.raw.hc7, R.raw.hc8, R.raw.hc9,
            R.raw.hc10, R.raw.hc11, R.raw.hc12, R.raw.hc13, R.raw.hc14,
            R.raw.hc15, R.raw.hc16, R.raw.hc17, R.raw.hc18, R.raw.hc19,
            R.raw.hc20, R.raw.hc21, R.raw.hc22, R.raw.hc23
        )
        resIdMinutes10Just = intArrayOf(
            0, R.raw.mt1, R.raw.mt2, R.raw.mt3, R.raw.mt4, R.raw.mt5
        )
        resIdMinutes10Continue = intArrayOf(
            0, R.raw.mc1, R.raw.mc2, R.raw.mc3, R.raw.mc4, R.raw.mc5
        )
        resIdMinutes1 = intArrayOf(
            0, R.raw.mn1, R.raw.mn2, R.raw.mn3, R.raw.mn4,
            R.raw.mn5, R.raw.mn6, R.raw.mn7, R.raw.mn8, R.raw.mn9
        )
    }
}
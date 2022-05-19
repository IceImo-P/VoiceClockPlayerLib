package net.imoya.android.voiceclock.player.sequence

import net.imoya.android.media.audio.resource.ResourceAudioSequenceItem
import net.imoya.android.voiceclock.player.property.VoiceClockPlayerProperty
import java.util.ArrayList

/**
 * 12時間制(1～12時)で時刻を読み上げる、PlayerTaskのリストを生成します。
 *
 * @param property  [VoiceClockPlayerProperty]
 * @param hourOfDay hour of day(0-23)
 * @param minute    minute(0-59)
 *
 * @author IceImo-P
 */
class DefaultSequenceFactory(property: VoiceClockPlayerProperty, hourOfDay: Int, minute: Int) :
    ResourceSequenceFactory(property) {
    private val hour: Int
    private val minute: Int

    init {
        hour = normalizeHour(hourOfDay)
        this.minute = minute
    }

    private fun normalizeHour(hour: Int): Int {
        var h = hour
        while (h > 12) {
            h -= 12
        }
        if (h == 0) return 12
        return h
    }

    override val list: Array<ResourceAudioSequenceItem>
        get() {
            val list = ArrayList<ResourceAudioSequenceItem>(3)
            if (minute == 0) {
                // x時です。
                list.add(ResourceAudioSequenceItem(0, resIdHourJust[hour]))
            } else if (minute < 10) {
                // x時
                list.add(ResourceAudioSequenceItem(0, resIdHourContinue[hour]))
                // x分です。
                list.add(
                    ResourceAudioSequenceItem(
                        defaultDelay + voiceDelay + delayAfterHours,
                        resIdMinutes1[minute]
                    )
                )
            } else if (minute % 10 == 0) {
                // x時
                list.add(ResourceAudioSequenceItem(0, resIdHourContinue[hour]))
                // x十分です。
                list.add(
                    ResourceAudioSequenceItem(
                        defaultDelay + voiceDelay + delayAfterHours,
                        resIdMinutes10Just[minute / 10]
                    )
                )
            } else {
                // x時
                list.add(ResourceAudioSequenceItem(0, resIdHourContinue[hour]))
                // x十
                list.add(
                    ResourceAudioSequenceItem(
                        defaultDelay + voiceDelay + delayAfterHours,
                        resIdMinutes10Continue[minute / 10]
                    )
                )
                // x分です。
                list.add(
                    ResourceAudioSequenceItem(
                        defaultDelay + voiceDelay,
                        resIdMinutes1[minute % 10]
                    )
                )
            }
            return list.toTypedArray()
        }

    override val parameters: String
        get() = "h=$hour, m=$minute"
}
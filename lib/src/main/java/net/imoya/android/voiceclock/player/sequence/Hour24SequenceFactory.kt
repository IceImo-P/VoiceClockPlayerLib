package net.imoya.android.voiceclock.player.sequence

import net.imoya.android.media.audio.resource.ResourceAudioSequenceItem
import net.imoya.android.voiceclock.player.property.VoiceClockPlayerProperty

/**
 * 24時間制(0～23時)で時刻を読み上げるTaskのリストを生成します。
 *
 * @param property  [VoiceClockPlayerProperty]
 * @param hourOfDay hour of date(0-23)
 * @param minute    minute(0-59)
 *
 * @author IceImo-P
 */
class Hour24SequenceFactory(property: VoiceClockPlayerProperty, hourOfDay: Int, minute: Int) :
    ResourceSequenceFactory(property) {
    private val hour: Int
    private val minute: Int

    init {
        hour = normalizeHour(hourOfDay)
        this.minute = minute
    }

    private fun normalizeHour(hour: Int): Int {
        var h = hour
        while (h > 23) {
            h -= 24
        }
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
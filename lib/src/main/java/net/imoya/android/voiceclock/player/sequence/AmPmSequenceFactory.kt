package net.imoya.android.voiceclock.player.sequence

import net.imoya.android.media.audio.resource.ResourceAudioSequenceItem
import net.imoya.android.voiceclock.player.R
import net.imoya.android.voiceclock.player.property.VoiceClockPlayerProperty
import java.util.*

/**
 * 12時間制(午前/午後 + 0～11時)で時刻を読み上げるTaskのリストを生成します。
 *
 * @param property  [VoiceClockPlayerProperty]
 * @param amPm      [Calendar.AM] or Calendar.PM
 * @param hourOfDay hour of day(0-23)
 * @param minute    minute(0-59)
 *
 * @author IceImo-P
 */
class AmPmSequenceFactory(
    property: VoiceClockPlayerProperty,
    amPm: Int,
    hourOfDay: Int,
    minute: Int
) : ResourceSequenceFactory(
    property
) {
    private val resIdAm: Int
    private val resIdPm: Int
    private val amPm: Int
    private val hour: Int
    private val minute: Int

    init {
        require(!(amPm != Calendar.AM && amPm != Calendar.PM)) { "amPm is neither Calendar.AM nor Calendar.PM" }
        resIdAm = R.raw.am
        resIdPm = R.raw.pm
        this.amPm = amPm
        hour = normalizeHour(hourOfDay)
        this.minute = minute
    }

    private fun normalizeHour(hour: Int): Int {
        var h = hour
        while (h > 11) {
            h -= 12
        }
        return h
    }

    override val list: Array<ResourceAudioSequenceItem>
        get() {
            val list = ArrayList<ResourceAudioSequenceItem>(4)
            list.add(
                ResourceAudioSequenceItem(
                    0,
                    if (amPm == Calendar.AM) resIdAm else resIdPm
                )
            )
            if (minute == 0) {
                // x時です。
                list.add(
                    ResourceAudioSequenceItem(
                        defaultDelay + voiceDelay + delayAfterAmPm,
                        resIdHourJust[hour]
                    )
                )
            } else if (minute < 10) {
                // x時
                list.add(
                    ResourceAudioSequenceItem(
                        defaultDelay + voiceDelay + delayAfterAmPm,
                        resIdHourContinue[hour]
                    )
                )
                // x分です。
                list.add(
                    ResourceAudioSequenceItem(
                        defaultDelay + delayAfterHours + voiceDelay,
                        resIdMinutes1[minute]
                    )
                )
            } else if (minute % 10 == 0) {
                // x時
                list.add(
                    ResourceAudioSequenceItem(
                        defaultDelay + voiceDelay + delayAfterAmPm,
                        resIdHourContinue[hour]
                    )
                )
                // x十分です。
                list.add(
                    ResourceAudioSequenceItem(
                        defaultDelay + voiceDelay + delayAfterHours,
                        resIdMinutes10Just[minute / 10]
                    )
                )
            } else {
                // x時
                list.add(
                    ResourceAudioSequenceItem(
                        defaultDelay + voiceDelay + delayAfterAmPm,
                        resIdHourContinue[hour]
                    )
                )
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
        get() = "amPm=$amPm, h=$hour, m=$minute"
}
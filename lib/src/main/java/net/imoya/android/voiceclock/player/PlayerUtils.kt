package net.imoya.android.voiceclock.player

import android.content.Context
import net.imoya.android.media.audio.raw.RawAudioSequenceItem
import net.imoya.android.media.audio.raw.ResourcesToRawConverter
import net.imoya.android.media.audio.resource.ResourceAudioSequenceItem

object PlayerUtils {
    @JvmStatic
    @Throws(Exception::class)
    fun convertResourcesToRawAudioTasks(
        context: Context, tasks: Array<ResourceAudioSequenceItem>
    ): Array<RawAudioSequenceItem> {
//        final int[] resourceIdList = Arrays.stream(tasks).mapToInt(task -> task.resourceId).toArray();
        val resourceIdList: IntArray = tasks.map { it.resourceId }.toIntArray()
        val converter = ResourcesToRawConverter(
            context, resourceIdList
        )
        converter.convert()
//        val rawAudioList = converter.result
//        val rawTasks = arrayOfNulls<RawAudioSequenceItem>(rawAudioList.size)
//        for (i in rawAudioList.indices) {
//            rawTasks[i] = RawAudioSequenceItem(tasks[i].delayMilliSeconds, rawAudioList[i])
//        }
//        return rawTasks
        return converter.result.mapIndexed { index, rawAudio ->
            RawAudioSequenceItem(
                tasks[index].delayMilliSeconds,
                rawAudio
            )
        }.toTypedArray()
    }
}
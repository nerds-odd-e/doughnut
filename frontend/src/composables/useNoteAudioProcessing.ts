import { ref, type Ref } from "vue"
import type { Note } from "@generated/donut-backend-api"
import { audioChunkToText } from "@/composables/audioChunkToText"
import type { AudioChunk } from "@/models/audio/audioProcessingScheduler"
import { useNoteStore } from "@/store/noteStore"

export function useNoteAudioProcessing(
  note: Note,
  errors: Ref<Record<string, string | undefined> | undefined>
) {
  const noteStore = useNoteStore()
  const noteId = note.id
  const isProcessing = ref(false)

  const processAudio = async (
    chunk: AudioChunk
  ): Promise<string | undefined> => {
    isProcessing.value = true
    try {
      const { segmentTexts, endTimestamp } = await audioChunkToText(chunk)
      if (errors.value?.conversion) {
        errors.value = undefined
      }

      try {
        if (segmentTexts.length) {
          await noteStore.appendDictatedText(noteId, segmentTexts)
        }
      } catch (saveError) {
        errors.value = saveError as Record<string, string | undefined>
        return
      }

      return endTimestamp
    } catch (error) {
      errors.value = {
        conversion:
          "Could not turn your speech into text. Your recording is kept.",
      }
      throw error
    } finally {
      isProcessing.value = false
    }
  }

  return { processAudio, isProcessing }
}

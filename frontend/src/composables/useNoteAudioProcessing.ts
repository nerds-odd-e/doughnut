import { ref, type Ref } from "vue"
import type { Note } from "@generated/donut-backend-api"
import { AiAudioController } from "@generated/donut-backend-api/sdk.gen"
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
      const { data: response, error } = await AiAudioController.audioToText({
        body: {
          uploadAudioFile: chunk.data,
          midSpeech: chunk.isMidSpeech,
        },
      })

      if (error || !response) {
        throw new Error("Failed to process audio")
      }

      try {
        await noteStore.appendDictatedText(noteId, response.dictatedText)
      } catch (saveError) {
        errors.value = saveError as Record<string, string | undefined>
        return
      }

      return response.endTimestamp
    } catch (error) {
      errors.value = error as Record<string, string | undefined>
      throw error
    } finally {
      isProcessing.value = false
    }
  }

  return { processAudio, isProcessing }
}

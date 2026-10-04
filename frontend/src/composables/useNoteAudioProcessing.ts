import { ref, type Ref } from "vue"
import type { Note } from "@generated/donut-backend-api"
import { AiAudioController } from "@generated/donut-backend-api/sdk.gen"
import type { AudioChunk } from "@/models/audio/audioProcessingScheduler"
import { useNoteStore } from "@/store/noteStore"

const getLastContentChunk = (
  content: string | undefined,
  maxLength = 500
): string => {
  if (!content) return ""
  if (content.length <= maxLength) return content
  return `...${content.slice(-maxLength)}`
}

export function useNoteAudioProcessing(
  note: Note,
  processingInstructions: Ref<string>,
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
      const realm = await noteStore.getOrLoadNoteRealm(noteId)
      const { data: response, error } = await AiAudioController.audioToText({
        body: {
          uploadAudioFile: chunk.data,
          additionalProcessingInstructions: processingInstructions.value,
          midSpeech: chunk.isMidSpeech,
          previousNoteContentToAppendTo: getLastContentChunk(
            realm.note.content
          ),
        },
      })

      if (error || !response) {
        throw new Error("Failed to process audio")
      }

      await noteStore.appendDictatedText(noteId, response.dictatedText)

      return response.endTimestamp
    } catch (error) {
      errors.value = error as Record<string, string | undefined>
      return
    } finally {
      isProcessing.value = false
    }
  }

  return { processAudio, isProcessing }
}

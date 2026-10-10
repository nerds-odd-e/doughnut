import { ref } from "vue"
import type { Note } from "@generated/donut-backend-api"
import { audioChunkToText } from "@/composables/audioChunkToText"
import { useToast } from "@/composables/useToast"
import type { AudioChunk } from "@/models/audio/audioProcessingScheduler"
import { useNoteStore } from "@/store/noteStore"

export function useNoteAudioProcessing(note: Note) {
  const noteStore = useNoteStore()
  const { showErrorToast } = useToast()
  const noteId = note.id
  const isProcessing = ref(false)
  const lastConversionFailed = ref(false)

  const convert = async (chunk: AudioChunk) => {
    try {
      const converted = await audioChunkToText(chunk)
      lastConversionFailed.value = false
      return converted
    } catch (error) {
      lastConversionFailed.value = true
      showErrorToast(
        chunk.isMidSpeech
          ? "Could not turn your speech into text. Your recording is kept."
          : "Could not turn your speech into text. Your recording is kept until you close Audio tools."
      )
      throw error
    }
  }

  const processAudio = async (
    chunk: AudioChunk
  ): Promise<string | undefined> => {
    isProcessing.value = true
    try {
      const { segmentTexts, endTimestamp } = await convert(chunk)
      try {
        if (segmentTexts.length) {
          await noteStore.appendDictatedText(noteId, segmentTexts)
        }
      } catch {
        // The failed save has shown its own toast; its audio is converted.
        return
      }
      return endTimestamp
    } finally {
      isProcessing.value = false
    }
  }

  return { processAudio, isProcessing, lastConversionFailed }
}

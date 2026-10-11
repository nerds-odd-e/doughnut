import { ref, type Ref } from "vue"
import type { Note } from "@generated/donut-backend-api"
import { audioChunkToText } from "@/composables/audioChunkToText"
import { useToast } from "@/composables/useToast"
import { SPEECH_NOT_CONVERTED_MESSAGE } from "@/composables/voiceInputFailureMessages"
import type { AudioChunk } from "@/models/audio/audioProcessingScheduler"
import { useNoteStore } from "@/store/noteStore"

export function useNoteAudioProcessing(
  note: Note,
  authorHasLeft: Ref<boolean>
) {
  const noteStore = useNoteStore()
  const { showErrorToast } = useToast()
  const noteId = note.id
  const lastConversionFailed = ref(false)

  const conversionFailure = (chunk: AudioChunk) => {
    const failure = SPEECH_NOT_CONVERTED_MESSAGE
    if (authorHasLeft.value) return failure
    if (chunk.isMidSpeech) return `${failure} Your recording is kept.`
    return `${failure} Your recording is kept until you leave this note; click Voice input to try again.`
  }

  const convert = async (chunk: AudioChunk) => {
    try {
      const converted = await audioChunkToText(chunk)
      lastConversionFailed.value = false
      return converted
    } catch (error) {
      lastConversionFailed.value = true
      showErrorToast(conversionFailure(chunk))
      throw error
    }
  }

  const processAudio = async (
    chunk: AudioChunk
  ): Promise<string | undefined> => {
    const { segmentTexts, endTimestamp } = await convert(chunk)
    try {
      if (segmentTexts.length) {
        await noteStore.addDictatedText(noteId, segmentTexts)
      }
    } catch {
      // The failed save has shown its own toast; its audio is converted.
      return
    }
    return endTimestamp
  }

  return { processAudio, lastConversionFailed }
}

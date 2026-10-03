import { ref, type Ref } from "vue"
import type { Note } from "@generated/donut-backend-api"
import {
  AiAudioController,
  AiController,
} from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { AudioChunk } from "@/models/audio/audioProcessingScheduler"
import { useNoteStore } from "@/store/noteStore"

const isPowerOfTwo = (n: number): boolean => n > 0 && (n & (n - 1)) === 0

const shouldSuggestTitle = (callCount: number): boolean =>
  isPowerOfTwo(callCount)

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
  const callCount = ref(0)

  const updateTopicIfSuggested = async (noteId: number) => {
    const { data: suggestedTopic, error } = await apiCallWithLoading(() =>
      AiController.suggestTitle({
        path: { note: noteId },
      })
    )
    if (!error && suggestedTopic?.title) {
      await noteStore.updateTextField(
        noteId,
        "edit title",
        suggestedTopic.title
      )
    }
  }

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
          isMidSpeech: chunk.isMidSpeech,
          previousNoteContentToAppendTo: getLastContentChunk(
            realm.note.content
          ),
        },
      })

      if (error || !response) {
        throw new Error("Failed to process audio")
      }

      await noteStore.appendDictatedText(noteId, response.completionFromAudio)

      callCount.value++
      if (shouldSuggestTitle(callCount.value)) {
        updateTopicIfSuggested(noteId)
      }

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

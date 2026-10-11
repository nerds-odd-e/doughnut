import { computed, onBeforeUnmount, ref } from "vue"
import type { Note } from "@generated/donut-backend-api"
import {
  beginOpenNoteContentDictation,
  endOpenNoteContentDictation,
} from "@/composables/noteContentMutationBarrier"
import { useNoteAudioProcessing } from "@/composables/useNoteAudioProcessing"
import { useToast } from "@/composables/useToast"
import { MICROPHONE_UNAVAILABLE_MESSAGE } from "@/composables/voiceInputFailureMessages"
import { createAudioRecorder } from "@/models/audio/audioRecorder"
import { createWakeLocker } from "@/models/wakeLocker"

/**
 * True while the note on the page has a recording that runs, is being
 * finished, or is kept for a retry.
 */
export const voiceInputIsActive = ref(false)

export function useNoteVoiceInput(note: Note) {
  const phase = ref<"ready" | "recording" | "stopping" | "notConverted">(
    "ready"
  )
  const isRecording = computed(() => phase.value === "recording")
  const hasKeptRecording = computed(() => phase.value === "notConverted")
  const wakeLocker = createWakeLocker()
  const { showErrorToast } = useToast()

  const authorHasLeft = ref(false)

  const { processAudio, lastConversionFailed } = useNoteAudioProcessing(
    note,
    authorHasLeft
  )

  const audioRecorder = createAudioRecorder(processAudio)

  const start = async () => {
    try {
      await wakeLocker.request()
      await audioRecorder.startRecording()
      beginOpenNoteContentDictation(note.id)
      phase.value = "recording"
      voiceInputIsActive.value = true
    } catch {
      showErrorToast(MICROPHONE_UNAVAILABLE_MESSAGE)
      await wakeLocker.release()
    }
  }

  const stop = async () => {
    phase.value = "stopping"
    try {
      await audioRecorder.stopRecording()
    } finally {
      endOpenNoteContentDictation(note.id)
      phase.value =
        lastConversionFailed.value && audioRecorder.hasUnconvertedAudio()
          ? "notConverted"
          : "ready"
      if (!authorHasLeft.value) {
        voiceInputIsActive.value = hasKeptRecording.value
      }
      await wakeLocker.release()
    }
  }

  const retry = () => {
    beginOpenNoteContentDictation(note.id)
    return stop()
  }

  onBeforeUnmount(async () => {
    authorHasLeft.value = true
    voiceInputIsActive.value = false
    if (isRecording.value) {
      await stop()
    }
  })

  return {
    phase,
    isRecording,
    hasKeptRecording,
    audioRecorder,
    wakeLocker,
    processAudio,
    start,
    stop,
    retry,
  }
}

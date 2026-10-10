import { computed, onBeforeUnmount, ref } from "vue"
import type { Note } from "@generated/donut-backend-api"
import { useNoteAudioProcessing } from "@/composables/useNoteAudioProcessing"
import { useToast } from "@/composables/useToast"
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
      phase.value = "recording"
      voiceInputIsActive.value = true
    } catch {
      showErrorToast(
        "Could not use the microphone. Allow microphone access in your browser, then try again."
      )
      await wakeLocker.release()
    }
  }

  const stop = async () => {
    phase.value = "stopping"
    try {
      await audioRecorder.stopRecording()
    } finally {
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
    retry: stop,
  }
}

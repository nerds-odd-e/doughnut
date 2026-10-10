import { computed, onBeforeUnmount, ref } from "vue"
import type { Note } from "@generated/donut-backend-api"
import { useNoteAudioProcessing } from "@/composables/useNoteAudioProcessing"
import { useToast } from "@/composables/useToast"
import { createAudioRecorder } from "@/models/audio/audioRecorder"
import { createWakeLocker } from "@/models/wakeLocker"

/** True from the start of a recording until its last speech has been added. */
export const voiceInputIsActive = ref(false)

export function useNoteVoiceInput(note: Note) {
  const phase = ref<"ready" | "recording" | "stopping" | "notConverted">(
    "ready"
  )
  const isRecording = computed(() => phase.value === "recording")
  const wakeLocker = createWakeLocker()
  const { showErrorToast } = useToast()

  const { processAudio, lastConversionFailed } = useNoteAudioProcessing(note)

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
      voiceInputIsActive.value = false
      await wakeLocker.release()
    }
  }

  onBeforeUnmount(() => {
    if (isRecording.value) {
      stop()
    }
  })

  return {
    phase,
    isRecording,
    audioRecorder,
    wakeLocker,
    processAudio,
    start,
    stop,
  }
}

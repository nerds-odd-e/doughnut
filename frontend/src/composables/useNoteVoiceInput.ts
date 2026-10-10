import { computed, onBeforeUnmount, ref } from "vue"
import type { Note } from "@generated/donut-backend-api"
import { useNoteAudioProcessing } from "@/composables/useNoteAudioProcessing"
import { useToast } from "@/composables/useToast"
import { createAudioRecorder } from "@/models/audio/audioRecorder"
import { createWakeLocker } from "@/models/wakeLocker"

export function useNoteVoiceInput(note: Note) {
  const phase = ref<"ready" | "recording" | "stopping" | "notConverted">(
    "ready"
  )
  const isRecording = computed(() => phase.value === "recording")
  const hasKeptRecording = computed(() => phase.value === "notConverted")
  const wakeLocker = createWakeLocker()
  const { showErrorToast } = useToast()

  const { processAudio, isProcessing, lastConversionFailed } =
    useNoteAudioProcessing(note)

  const audioRecorder = createAudioRecorder(processAudio)

  const switchAudioDevice = async (deviceId: string) => {
    try {
      await audioRecorder.switchAudioDevice(deviceId)
    } catch {
      showErrorToast("Failed to switch audio device")
    }
  }

  const start = async () => {
    try {
      await wakeLocker.request()
      await audioRecorder.startRecording()
      phase.value = "recording"
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
    hasKeptRecording,
    isProcessing,
    audioRecorder,
    wakeLocker,
    processAudio,
    audioDevices: audioRecorder.getAudioDevices(),
    selectedDevice: audioRecorder.getSelectedDevice(),
    start,
    stop,
    retry: stop,
    tryFlush: () => audioRecorder.tryFlush(),
    switchAudioDevice,
  }
}

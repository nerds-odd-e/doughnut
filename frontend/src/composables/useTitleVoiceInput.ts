import { computed, onUnmounted, ref } from "vue"
import { audioChunkToText } from "@/composables/audioChunkToText"
import { useToast } from "@/composables/useToast"
import {
  MICROPHONE_UNAVAILABLE_MESSAGE,
  SPEECH_NOT_CONVERTED_MESSAGE,
} from "@/composables/voiceInputFailureMessages"
import {
  createAudioRecorder,
  type AudioRecorder,
} from "@/models/audio/audioRecorder"

/** Title speech converts on Stop; the editor owns where the words are inserted. */
export function useTitleVoiceInput(
  heardSegments: (segments: string[]) => void,
  beforeStart: () => void
) {
  const { showErrorToast } = useToast()
  const phase = ref<"idle" | "starting" | "listening" | "converting">("idle")
  const busy = computed(() => phase.value !== "idle")
  let audioRecorder: AudioRecorder | undefined

  const startListening = async () => {
    beforeStart()
    phase.value = "starting"
    const recorder = createAudioRecorder(
      async (chunk) => {
        try {
          const { segmentTexts, endTimestamp } = await audioChunkToText(
            chunk,
            true
          )
          if (audioRecorder === recorder && segmentTexts.length) {
            heardSegments(segmentTexts)
          }
          return endTimestamp
        } catch {
          showErrorToast(SPEECH_NOT_CONVERTED_MESSAGE)
          return
        }
      },
      { convertOnlyAtStop: true }
    )
    audioRecorder = recorder
    try {
      await recorder.startRecording()
      if (audioRecorder === recorder) phase.value = "listening"
    } catch {
      if (audioRecorder === recorder) {
        audioRecorder = undefined
        phase.value = "idle"
        showErrorToast(MICROPHONE_UNAVAILABLE_MESSAGE)
      }
    }
  }

  const stopListening = async () => {
    const recorder = audioRecorder
    phase.value = "converting"
    try {
      await recorder?.stopRecording()
    } finally {
      if (audioRecorder === recorder) {
        phase.value = "idle"
        audioRecorder = undefined
      }
    }
  }

  const leave = () => {
    const recorder = audioRecorder
    audioRecorder = undefined
    phase.value = "idle"
    recorder?.stopRecording()
  }

  onUnmounted(leave)

  return {
    phase,
    busy,
    startListening,
    stopListening,
    leave,
    onControlClick: () =>
      phase.value === "listening" ? stopListening() : startListening(),
  }
}

export type TitleVoiceInput = ReturnType<typeof useTitleVoiceInput>

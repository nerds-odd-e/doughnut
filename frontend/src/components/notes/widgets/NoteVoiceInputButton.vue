<template>
  <VoiceInputButton
    :class="
      hasKeptRecording
        ? toolbarKeptRecordingBtnClass
        : toolbarToggleBtnClass(isRecording)
    "
    :state="state"
    :read-level="readLevel"
    :names="noteVoiceInputTitles"
    @pointerdown="preserveTitleFocus"
    @click="click"
  />
</template>

<script setup lang="ts">
import { computed, inject } from "vue"
import type { Note } from "@generated/donut-backend-api"
import VoiceInputButton from "@/components/commons/VoiceInputButton.vue"
import { useNoteVoiceInput } from "@/composables/useNoteVoiceInput"
import { noteTitleDictationKey } from "@/composables/useTitleDictation"
import { noteVoiceInputTitles } from "./noteMoreOptionsTitles"
import {
  toolbarKeptRecordingBtnClass,
  toolbarToggleBtnClass,
} from "./noteToolbarButtonClasses"

const { note } = defineProps<{ note: Note }>()

const {
  phase,
  isRecording: contentRecording,
  hasKeptRecording,
  audioRecorder,
  wakeLocker,
  processAudio,
  start: startContent,
  stop,
  retry,
} = useNoteVoiceInput(note)

const title = inject(noteTitleDictationKey, undefined)
const titleState = computed(() => title?.dictation.voiceInput.buttonState.value)
const titleRecording = computed(() => titleState.value === "recording")
const isRecording = computed(
  () => titleRecording.value || contentRecording.value
)
const startingOrFinishing = computed(
  () =>
    phase.value === "starting" ||
    phase.value === "stopping" ||
    titleState.value === "busy"
)
const preserveTitleFocus = (event: PointerEvent) => {
  if (
    !isRecording.value &&
    !hasKeptRecording.value &&
    title?.hasTitleIntent.value
  )
    event.preventDefault()
}
const start = () =>
  title?.hasTitleIntent.value
    ? title.dictation.voiceInput.startListening()
    : startContent()

const state = computed(() => {
  if (isRecording.value) return "recording"
  if (startingOrFinishing.value) return "busy"
  if (hasKeptRecording.value) return "keptRecording"
  return "idle"
})
const readLevel = () =>
  titleRecording.value
    ? title!.dictation.voiceInput.readLevel()
    : audioRecorder.getAudioData()

const click = () => {
  if (titleRecording.value) return title!.dictation.voiceInput.stopListening()
  if (isRecording.value) return stop()
  if (hasKeptRecording.value) return retry()
  return start()
}

defineExpose({ start, wakeLocker, processAudio })
</script>

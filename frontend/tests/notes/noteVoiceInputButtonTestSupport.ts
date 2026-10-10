import NoteVoiceInputButton from "@/components/notes/widgets/NoteVoiceInputButton.vue"
import { noteVoiceInputTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import type { AudioChunk } from "@/models/audio/audioProcessingScheduler"
import type { Note } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockShowNote } from "@tests/helpers"
import {
  clearAudioHardwareMocks,
  installAudioBrowserSpies,
} from "@tests/notes/noteVoiceInputButtonMocks"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, expect, vi } from "vitest"
import type { ComponentPublicInstance } from "vue"

export type NoteVoiceInputButtonWrapper = VueWrapper<ComponentPublicInstance>

export type NoteVoiceInputVm = {
  audioRecorder: {
    startRecording: ReturnType<typeof vi.fn>
    stopRecording: ReturnType<typeof vi.fn>
    hasUnconvertedAudio: ReturnType<typeof vi.fn>
    getAudioData: ReturnType<typeof vi.fn>
  }
  wakeLocker: {
    request: ReturnType<typeof vi.fn>
    release: ReturnType<typeof vi.fn>
  }
  isRecording: boolean
  processAudio: (chunk: AudioChunk) => Promise<string | undefined>
}

export function voiceInputVm(
  wrapper: NoteVoiceInputButtonWrapper
): NoteVoiceInputVm {
  return wrapper.vm as unknown as NoteVoiceInputVm
}

export function audioChunk(
  data: File = new File([], "test.webm"),
  isMidSpeech = false
): AudioChunk {
  return { data, isMidSpeech }
}

export function midSpeechChunk(
  data: File = new File([], "test.webm")
): AudioChunk {
  return audioChunk(data, true)
}

export function audioTextResponse(
  passage: string | string[],
  endTimestamp = "00:00:37,270"
) {
  return {
    segmentTexts: typeof passage === "string" ? [passage] : passage,
    endTimestamp,
  }
}

export function processAudio(
  wrapper: NoteVoiceInputButtonWrapper,
  chunk: AudioChunk = audioChunk()
) {
  return voiceInputVm(wrapper).processAudio(chunk)
}

export function voiceInputButton(wrapper: NoteVoiceInputButtonWrapper) {
  return wrapper.find("button")
}

/** The button as an author finds it before and after a recording. */
export function expectIdleVoiceInputButton(
  wrapper: NoteVoiceInputButtonWrapper
) {
  const button = voiceInputButton(wrapper)
  expect(button.attributes("aria-label")).toBe(noteVoiceInputTitles.start)
  expect(button.attributes()).not.toHaveProperty("disabled")
  expect(button.classes()).not.toContain("daisy-btn-primary")
}

export function mountNoteVoiceInputButton(
  note: Note = makeMe.aNote.please(),
  options?: { attachToBody?: boolean }
): NoteVoiceInputButtonWrapper {
  return helper
    .component(NoteVoiceInputButton)
    .withCleanStorage()
    .withProps({ note })
    .mount(
      options?.attachToBody === false ? undefined : { attachTo: document.body }
    )
}

export async function startRecording(wrapper: NoteVoiceInputButtonWrapper) {
  await wrapper
    .find(`button[aria-label="${noteVoiceInputTitles.start}"]`)
    .trigger("click")
  await flushPromises()
  await wrapper.vm.$nextTick()
}

export async function stopRecording(wrapper: NoteVoiceInputButtonWrapper) {
  await wrapper
    .find(`button[aria-label="${noteVoiceInputTitles.stop}"]`)
    .trigger("click")
  await flushPromises()
  await wrapper.vm.$nextTick()
}

/** Shared lifecycle for NoteVoiceInputButton capability specs (call after vi.mock blocks). */
export function useNoteVoiceInputTestLifecycle() {
  beforeEach(() => {
    vi.useFakeTimers()
    mockShowNote()
    installAudioBrowserSpies()
    clearAudioHardwareMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()
  })
}

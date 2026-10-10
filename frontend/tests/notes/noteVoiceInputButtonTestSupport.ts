import NoteVoiceInputButton from "@/components/notes/widgets/NoteVoiceInputButton.vue"
import { noteVoiceInputTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { createAudioRecorder } from "@/models/audio/audioRecorder"
import type { AudioChunk } from "@/models/audio/audioProcessingScheduler"
import { useNoteStore } from "@/store/noteStore"
import type { Note, NoteRealm } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockShowNote } from "@tests/helpers"
import {
  clearAudioHardwareMocks,
  installAudioBrowserSpies,
} from "@tests/notes/noteVoiceInputButtonMocks"
import {
  mountNoteToolbar,
  noteToolbarAction,
  noteToolbarProps,
} from "@tests/notes/noteToolbarTestHelpers"
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

/**
 * A note toolbar whose Voice input is recording. Stopping the returned
 * `recorder` converts the remaining audio.
 */
export async function mountNoteToolbarRecording(realm: NoteRealm) {
  const wrapper = await mountNoteToolbar(realm)
  useNoteStore().refreshNoteRealm(realm)
  await noteToolbarAction(wrapper, noteVoiceInputTitles.start).trigger("click")
  await flushPromises()
  const recorderMock = vi.mocked(createAudioRecorder).mock
  const convert = recorderMock.lastCall![0]
  const recorder = vi.mocked(recorderMock.results.at(-1)!.value)
  recorder.stopRecording.mockImplementation(async () => {
    await convert(audioChunk()).catch(() => undefined)
  })
  return { wrapper, recorder }
}

/** The author moves from the toolbar's note to another note. */
export async function moveNoteToolbarTo(wrapper: VueWrapper, realm: NoteRealm) {
  useNoteStore().refreshNoteRealm(realm)
  await wrapper.setProps(noteToolbarProps(realm))
  await flushPromises()
}

/** The toolbar offers idle Voice input and nothing of a recording. */
export function expectIdleVoiceInputInToolbar(wrapper: VueWrapper) {
  expect(noteToolbarAction(wrapper, noteVoiceInputTitles.stop).exists()).toBe(
    false
  )
  expect(noteToolbarAction(wrapper, noteVoiceInputTitles.retry).exists()).toBe(
    false
  )
  const button = noteToolbarAction(wrapper, noteVoiceInputTitles.start)
  expect(button.attributes()).not.toHaveProperty("disabled")
  expect(button.attributes()).not.toHaveProperty("aria-pressed")
  expect(button.classes()).not.toContain("daisy-btn-primary")
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

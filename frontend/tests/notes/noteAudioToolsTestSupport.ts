import NoteAudioTools from "@/components/notes/widgets/NoteAudioTools.vue"
import type { AudioChunk } from "@/models/audio/audioProcessingScheduler"
import type { Note } from "@generated/doughnut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockShowNote } from "@tests/helpers"
import {
  clearAudioHardwareMocks,
  installAudioBrowserSpies,
} from "@tests/notes/noteAudioToolsMocks"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, vi } from "vitest"
import type { ComponentPublicInstance } from "vue"

export type NoteAudioToolsWrapper = VueWrapper<ComponentPublicInstance>

export type NoteAudioToolsVm = {
  audioRecorder: {
    startRecording: ReturnType<typeof vi.fn>
    stopRecording: ReturnType<typeof vi.fn>
    tryFlush: ReturnType<typeof vi.fn>
    switchAudioDevice: ReturnType<typeof vi.fn>
  }
  wakeLocker: {
    request: ReturnType<typeof vi.fn>
    release: ReturnType<typeof vi.fn>
  }
  isRecording: boolean
  audioFile: File | null
  errors: Record<string, string> | null
  processAudio: (chunk: AudioChunk) => Promise<string | undefined>
}

export function audioToolsVm(wrapper: NoteAudioToolsWrapper): NoteAudioToolsVm {
  return wrapper.vm as unknown as NoteAudioToolsVm
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

export function processAudio(
  wrapper: NoteAudioToolsWrapper,
  chunk: AudioChunk = audioChunk()
) {
  return audioToolsVm(wrapper).processAudio(chunk)
}

export function findButtonByTitle(
  wrapper: NoteAudioToolsWrapper,
  title: string
) {
  return wrapper
    .findAll("button")
    .find((button) => button.attributes("title") === title)
}

export function mountNoteAudioTools(
  note: Note = makeMe.aNote.please(),
  options?: { attachToBody?: boolean }
): NoteAudioToolsWrapper {
  return helper
    .component(NoteAudioTools)
    .withCleanStorage()
    .withProps({ note })
    .mount(
      options?.attachToBody === false ? undefined : { attachTo: document.body }
    )
}

export async function startRecording(wrapper: NoteAudioToolsWrapper) {
  await findButtonByTitle(wrapper, "Record Audio")!.trigger("click")
  await flushPromises()
  await wrapper.vm.$nextTick()
}

export async function stopRecording(wrapper: NoteAudioToolsWrapper) {
  await findButtonByTitle(wrapper, "Stop Recording")!.trigger("click")
  await flushPromises()
  await wrapper.vm.$nextTick()
}

/** Shared lifecycle for NoteAudioTools capability specs (call after vi.mock blocks). */
export function useNoteAudioToolsTestLifecycle() {
  beforeEach(() => {
    vi.useFakeTimers()
    mockShowNote()
    installAudioBrowserSpies()
    clearAudioHardwareMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()
    document.body.innerHTML = ""
  })
}

import { AiAudioController } from "@generated/donut-backend-api/sdk.gen"
import NoteEditableTitle from "@/components/notes/core/NoteEditableTitle.vue"
import { createAudioRecorder } from "@/models/audio/audioRecorder"
import type { NoteTopology } from "@generated/donut-backend-api"
import type { ComponentPublicInstance } from "vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import helper, { mockSdkService, wrapSdkError } from "@tests/helpers"
import {
  clearAudioHardwareMocks,
  installAudioBrowserSpies,
} from "@tests/notes/noteVoiceInputButtonMocks"
import { setCaretInEditor } from "@tests/components/form/seamlessTextEditorTestSupport"
import { audioTextResponse } from "@tests/notes/noteVoiceInputButtonTestSupport"
import { expectDictationMarkerAt } from "@tests/notes/dictationMarkerTestSupport"
import { titleEditorEl } from "@tests/notes/noteTextContentTestSupport"
import { flushReferencedTitleBlurDiscardCheck } from "@tests/notes/textContentWrapperTestSupport"
import { afterEach, beforeEach, expect, vi } from "vitest"

/** The microphone button, found by the accessible name it has now. */
export function speakTitleButton(
  wrapper: VueWrapper<ComponentPublicInstance>,
  name: "Speak the title" | "Stop speaking the title"
) {
  return wrapper.find(`button[aria-label="${name}"]`)
}

/** The microphone button waits for a click to start listening. */
export function expectIdleSpeakTitleButton(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  const button = speakTitleButton(wrapper, "Speak the title")
  expect(button.attributes("title")).toBe("Speak the title")
  expect(button.attributes()).not.toHaveProperty("aria-pressed")
  expect(button.attributes()).not.toHaveProperty("disabled")
}

/** The microphone button is highlighted and a click stops listening. */
export function expectListeningSpeakTitleButton(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  const button = speakTitleButton(wrapper, "Stop speaking the title")
  expect(button.attributes("title")).toBe("Stop speaking the title")
  expect(button.attributes("aria-pressed")).toBe("true")
  expect(button.classes()).toEqual(
    expect.arrayContaining(["daisy-btn-soft", "daisy-btn-primary"])
  )
}

/** The microphone button is unavailable while speech becomes text. */
export function expectConvertingSpeakTitleButton(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  const button = speakTitleButton(wrapper, "Speak the title")
  expect(button.attributes()).toHaveProperty("disabled")
  expect(button.attributes()).not.toHaveProperty("aria-pressed")
}

export function mountNoteEditableTitle(options: {
  noteTopology: NoteTopology
  noteId: number
  readonly?: boolean
  hasInboundReferences?: boolean
}) {
  return helper
    .component(NoteEditableTitle)
    .withCleanStorage()
    .withProps({
      noteTopology: options.noteTopology,
      noteId: options.noteId,
      readonly: options.readonly ?? false,
      hasInboundReferences: options.hasInboundReferences ?? false,
    })
    .mount({ attachTo: document.body })
}

/** The next conversions return this transcript. */
export function hearing(transcript: string) {
  mockSdkService(
    AiAudioController,
    "audioToText",
    audioTextResponse(transcript)
  )
}

export function placeCaretInTitle(
  wrapper: VueWrapper<ComponentPublicInstance>,
  start: number,
  end = start
) {
  const title = titleEditorEl(wrapper)
  title.focus()
  setCaretInEditor(title, start, end)
}

export function selectWholeTitle(wrapper: VueWrapper<ComponentPublicInstance>) {
  const title = titleEditorEl(wrapper)
  title.focus()
  window.getSelection()!.selectAllChildren(title)
}

/** "false" while the title takes no edits. */
export function titleEditable(wrapper: VueWrapper<ComponentPublicInstance>) {
  return titleEditorEl(wrapper).getAttribute("contenteditable")
}

export function titleCaretOffset() {
  const range = window.getSelection()!.getRangeAt(0)
  expect(range.collapsed).toBe(true)
  return range.startOffset
}

/** Shared timers, audio mocks, and default transcription for spoken-title specs. */
export function useSpokenTitleTestLifecycle(
  defaultTranscript = "Photosynthesis in desert plants."
) {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.resetAllMocks()
    installAudioBrowserSpies()
    clearAudioHardwareMocks()
    hearing(defaultTranscript)
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })
}

export async function speakTheTitle(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  await speakTitleButton(wrapper, "Speak the title").trigger("click")
  await flushPromises()
}

export async function stopSpeaking(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  await speakTitleButton(wrapper, "Stop speaking the title").trigger("click")
  await flushPromises()
}

export async function speakAndStop(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  await speakTheTitle(wrapper)
  await stopSpeaking(wrapper)
}

function latestMockedAudioRecorder() {
  return vi.mocked(createAudioRecorder).mock.results[0]!.value as {
    stopRecording: ReturnType<typeof vi.fn>
  }
}

/** Hold Stop in converting until finishStop runs the processor callback. */
export function holdSpeakTitleConvertingUntilFinished() {
  const recorder = latestMockedAudioRecorder()
  const processAudio = vi.mocked(createAudioRecorder).mock.calls[0]![0]!
  let finishStop!: () => void
  recorder.stopRecording.mockImplementation(
    () =>
      new Promise<void>((resolve) => {
        finishStop = () => {
          processAudio({
            data: new File([], "test.webm"),
            isMidSpeech: false,
          }).then(() => resolve())
        }
      })
  )
  return { finishStop: () => finishStop() }
}

/** The next recorder fails to start. */
export function microphoneCannotStart() {
  const createRecorder = vi.mocked(createAudioRecorder)
  const createRecorderImpl = createRecorder.getMockImplementation()!
  createRecorder.mockImplementationOnce((callback, options) => {
    const recorder = createRecorderImpl(callback, options!)
    vi.mocked(recorder.startRecording).mockRejectedValueOnce(
      new Error("Permission denied")
    )
    return recorder
  })
}

/** Stop without invoking the processor — silent recording, no conversion. */
export function stubSilentStopRecording() {
  latestMockedAudioRecorder().stopRecording.mockImplementation(
    async () => undefined
  )
}

/** audioToText returns a response with no transcript segments. */
export function mockAudioToTextWithNoSegments() {
  return mockSdkService(AiAudioController, "audioToText", audioTextResponse([]))
}

/** First audioToText fails; later calls return the given transcript. */
export function mockAudioToTextFailThen(transcript: string | string[]) {
  const audioToTextMock = mockSdkService(
    AiAudioController,
    "audioToText",
    audioTextResponse(transcript)
  )
  audioToTextMock.mockResolvedValueOnce(wrapSdkError("API Error"))
  return audioToTextMock
}

/** Leave the title area so a linked rename can discard (same as typed). */
export async function blurAwayFromSpokenTitle(
  wrapper: VueWrapper<ComponentPublicInstance>
) {
  titleEditorEl(wrapper).blur()
  await flushPromises()
  await flushReferencedTitleBlurDiscardCheck()
}

/** Where the browser draws the title's text position `offset`. */
export function titleTextPlace(
  wrapper: VueWrapper<ComponentPublicInstance>,
  offset: number
) {
  const place = document.createRange()
  place.setStart(titleEditorEl(wrapper).firstChild!, offset)
  return place.getBoundingClientRect()
}

/** The marker is drawn right after the title's text position `offset`. */
export function expectTitleMarkerAfter(
  wrapper: VueWrapper<ComponentPublicInstance>,
  offset: number
) {
  expectDictationMarkerAt(titleTextPlace(wrapper, offset))
}

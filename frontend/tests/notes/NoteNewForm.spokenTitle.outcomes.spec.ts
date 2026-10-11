import { createAudioRecorder } from "@/models/audio/audioRecorder"
import type { VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import {
  isNoteNewFormSubmitDisabled,
  mountNoteNewForm,
  notebookRootProps,
  noteTitleText,
  setupNoteNewFormSdkMocks,
} from "@tests/notes/noteNewFormTestSupport"
import {
  expectIdleSpeakTitleButton,
  microphoneCannotStart,
  mockAudioToTextFailThen,
  mockAudioToTextWithNoSegments,
  speakAndStop,
  speakTheTitle,
  stopSpeaking,
  stubSilentStopRecording,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import {
  noToastShown,
  showToastsOnPage,
  toastMessage,
  toastShown,
} from "@tests/helpers/toastTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

vi.mock("@/models/audio/recorderWorklet", async () => {
  const { recorderWorkletMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return recorderWorkletMockExports()
})

vi.mock("@/models/audio/audioRecorder", async () => {
  const { audioRecorderInvokingCallbackMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return audioRecorderInvokingCallbackMockExports()
})

useSpokenTitleTestLifecycle()
showToastsOnPage()

describe("NoteNewForm spoken title outcomes", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>

  beforeEach(() => {
    setupNoteNewFormSdkMocks()
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("returns to idle without a message after a silent recording that runs no conversion", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await speakTheTitle(wrapper)
    stubSilentStopRecording()
    await stopSpeaking(wrapper)

    expectIdleSpeakTitleButton(wrapper)
    await noToastShown()
    expect(noteTitleText(wrapper)).toBe("Untitled")
    expect(isNoteNewFormSubmitDisabled(wrapper)).toBe(false)
  })

  it("returns to idle without a message when the response has no segments", async () => {
    mockAudioToTextWithNoSegments()
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })

    await speakAndStop(wrapper)

    expectIdleSpeakTitleButton(wrapper)
    await noToastShown()
    expect(noteTitleText(wrapper)).toBe("Untitled")
    expect(isNoteNewFormSubmitDisabled(wrapper)).toBe(false)
  })

  it("explains a microphone that cannot be used and keeps the idle button", async () => {
    microphoneCannotStart()

    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await speakTheTitle(wrapper)

    expect(toastMessage(await toastShown("error"))).toBe(
      "Could not use the microphone. Allow microphone access in your browser, then try again."
    )
    expectIdleSpeakTitleButton(wrapper)
    expect(isNoteNewFormSubmitDisabled(wrapper)).toBe(false)
  })

  it("explains a failed conversion, leaves the title alone, and speaks again with a fresh recorder", async () => {
    mockAudioToTextFailThen("only the second recording.")

    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await speakAndStop(wrapper)

    expect(toastMessage(await toastShown("error"))).toBe(
      "Could not turn your speech into text."
    )
    expect(noteTitleText(wrapper)).toBe("Untitled")
    expectIdleSpeakTitleButton(wrapper)
    expect(isNoteNewFormSubmitDisabled(wrapper)).toBe(false)
    expect(vi.mocked(createAudioRecorder)).toHaveBeenCalledTimes(1)

    await speakAndStop(wrapper)

    expect(vi.mocked(createAudioRecorder)).toHaveBeenCalledTimes(2)
    expect(noteTitleText(wrapper)).toBe("only the second recording.")
  })
})

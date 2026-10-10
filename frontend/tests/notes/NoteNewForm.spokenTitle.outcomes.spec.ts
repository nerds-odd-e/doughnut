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
  findSpeakTitleButtonByText,
  mockAudioToTextFailThen,
  mockAudioToTextWithNoSegments,
  speakAndStop,
  speakTheTitle,
  speakTitleStatus,
  speakTitleStatusNode,
  stopSpeaking,
  stubSilentStopRecording,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
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

describe("NoteNewForm spoken title outcomes", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>

  beforeEach(() => {
    setupNoteNewFormSdkMocks()
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("says nothing was heard after a silent recording that runs no conversion", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await speakTheTitle(wrapper)
    stubSilentStopRecording()
    await stopSpeaking(wrapper)

    expect(speakTitleStatus(wrapper)).toBe("No speech was turned into text.")
    expect(noteTitleText(wrapper)).toBe("Untitled")
    expect(isNoteNewFormSubmitDisabled(wrapper)).toBe(false)
  })

  it("says nothing was heard when the response has no segments", async () => {
    mockAudioToTextWithNoSegments()
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })

    await speakAndStop(wrapper)

    expect(speakTitleStatus(wrapper)).toBe("No speech was turned into text.")
    expect(noteTitleText(wrapper)).toBe("Untitled")
    expect(isNoteNewFormSubmitDisabled(wrapper)).toBe(false)
  })

  it("explains when the microphone cannot be used and keeps Speak the title", async () => {
    const createRecorder = vi.mocked(createAudioRecorder)
    const createRecorderImpl = createRecorder.getMockImplementation()!
    createRecorder.mockImplementationOnce((callback, options) => {
      const recorder = createRecorderImpl(callback, options!)
      vi.mocked(recorder.startRecording).mockRejectedValueOnce(
        new Error("Permission denied")
      )
      return recorder
    })

    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await speakTheTitle(wrapper)

    const status = speakTitleStatusNode(wrapper)
    expect(status.text()).toBe(
      "Could not use the microphone. Allow microphone access in your browser, then try again."
    )
    expect(status.classes()).toContain("text-error")
    expect(findSpeakTitleButtonByText(wrapper, "Speak the title")).toBeTruthy()
    expect(findSpeakTitleButtonByText(wrapper, "Stop")).toBeUndefined()
  })

  it("explains a failed conversion, leaves the title alone, and speaks again with a fresh recorder", async () => {
    mockAudioToTextFailThen("only the second recording.")

    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await speakAndStop(wrapper)

    const status = speakTitleStatusNode(wrapper)
    expect(status.text()).toBe("Could not turn your speech into text.")
    expect(status.classes()).toContain("text-error")
    expect(noteTitleText(wrapper)).toBe("Untitled")
    expect(isNoteNewFormSubmitDisabled(wrapper)).toBe(false)
    expect(findSpeakTitleButtonByText(wrapper, "Retry")).toBeUndefined()
    expect(vi.mocked(createAudioRecorder)).toHaveBeenCalledTimes(1)

    await speakAndStop(wrapper)

    expect(vi.mocked(createAudioRecorder)).toHaveBeenCalledTimes(2)
    expect(noteTitleText(wrapper)).toBe("only the second recording.")
  })
})

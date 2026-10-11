import type { VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import {
  mountNoteNewForm,
  notebookRootProps,
  noteTitleText,
  setNoteNewFormTitle,
  setupNoteNewFormSdkMocks,
} from "@tests/notes/noteNewFormTestSupport"
import {
  microphoneCannotStart,
  mockAudioToTextFailThen,
  mockAudioToTextWithNoSegments,
  placeCaretInTitle,
  speakAndStop,
  speakTheTitle,
  stopSpeaking,
  titleEditable,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import { settleScheduledAutofocus } from "@tests/helpers/focusTargetTestSupport"
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

useSpokenTitleTestLifecycle("weekly")

describe("The New note title while it is being spoken", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>

  beforeEach(async () => {
    setupNoteNewFormSdkMocks()
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await settleScheduledAutofocus()
  })

  afterEach(() => {
    wrapper.unmount()
  })

  it("is read-only while listening and editable once the words replaced the untouched default", async () => {
    placeCaretInTitle(wrapper, "Unt".length)

    await speakTheTitle(wrapper)
    expect(titleEditable(wrapper)).toBe("false")
    await stopSpeaking(wrapper)

    expect(noteTitleText(wrapper)).toBe("weekly")
    expect(titleEditable(wrapper)).toBe("true")
  })

  it("keeps the target a typed title had at the start when the selection moves", async () => {
    await setNoteNewFormTitle(wrapper, "Project review")
    placeCaretInTitle(wrapper, "Project".length)

    await speakTheTitle(wrapper)
    expect(titleEditable(wrapper)).toBe("false")
    placeCaretInTitle(wrapper, 0)
    await stopSpeaking(wrapper)

    expect(noteTitleText(wrapper)).toBe("Project weekly review")
  })

  it("stays editable when listening cannot start", async () => {
    microphoneCannotStart()

    await speakTheTitle(wrapper)

    expect(titleEditable(wrapper)).toBe("true")
  })

  it("is editable again when nothing was heard", async () => {
    mockAudioToTextWithNoSegments()

    await speakAndStop(wrapper)

    expect(titleEditable(wrapper)).toBe("true")
  })

  it("is editable again after a failed conversion", async () => {
    mockAudioToTextFailThen("weekly")

    await speakAndStop(wrapper)

    expect(titleEditable(wrapper)).toBe("true")
  })
})

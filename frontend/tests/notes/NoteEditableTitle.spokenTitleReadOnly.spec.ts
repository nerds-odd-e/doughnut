import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "donut-test-fixtures/makeMe"
import { noteTitleText } from "@tests/notes/noteNewFormTestSupport"
import {
  hearing,
  holdSpeakTitleConvertingUntilFinished,
  microphoneCannotStart,
  mockAudioToTextFailThen,
  mockAudioToTextWithNoSegments,
  mountNoteEditableTitle,
  placeCaretInTitle,
  speakAndStop,
  speakTheTitle,
  stopSpeaking,
  titleCaretOffset,
  titleEditable,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import {
  mockUpdateNoteTitle,
  titleEditorEl,
} from "@tests/notes/noteTextContentTestSupport"
import {
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

useSpokenTitleTestLifecycle("harvest")
showToastsOnPage()

describe("The title of an existing note while it is being spoken", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>

  beforeEach(() => {
    mockUpdateNoteTitle()
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })
    placeCaretInTitle(wrapper, "Orchard".length)
  })

  afterEach(() => {
    wrapper.unmount()
  })

  it("is read-only while listening and keeps its target when the selection moves", async () => {
    await speakTheTitle(wrapper)
    expect(titleEditable(wrapper)).toBe("false")
    placeCaretInTitle(wrapper, 0)

    await stopSpeaking(wrapper)

    expect(noteTitleText(wrapper)).toBe("Orchard harvest notes")
  })

  it("stays read-only until the words have arrived, then is editable with focus and the caret after them", async () => {
    await speakTheTitle(wrapper)
    const { finishStop } = holdSpeakTitleConvertingUntilFinished()

    await stopSpeaking(wrapper)
    expect(titleEditable(wrapper)).toBe("false")
    finishStop()
    await flushPromises()

    expect(titleEditable(wrapper)).toBe("true")
    expect(document.activeElement).toBe(titleEditorEl(wrapper))
    expect(titleCaretOffset()).toBe("Orchard harvest".length)
  })

  it("stays editable beside the microphone toast when listening cannot start", async () => {
    microphoneCannotStart()

    await speakTheTitle(wrapper)

    expect(toastMessage(await toastShown("error"))).toContain(
      "Could not use the microphone."
    )
    expect(titleEditable(wrapper)).toBe("true")
  })

  it("is editable again when nothing was heard", async () => {
    mockAudioToTextWithNoSegments()

    await speakAndStop(wrapper)

    expect(titleEditable(wrapper)).toBe("true")
  })

  it("is editable again beside the toast of a failed conversion", async () => {
    mockAudioToTextFailThen("harvest")

    await speakAndStop(wrapper)

    expect(toastMessage(await toastShown("error"))).toBe(
      "Could not turn your speech into text."
    )
    expect(titleEditable(wrapper)).toBe("true")
  })

  it("puts the words of a second session after those of the first", async () => {
    await speakAndStop(wrapper)

    hearing("and pruning")
    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Orchard harvest and pruning notes")
  })
})

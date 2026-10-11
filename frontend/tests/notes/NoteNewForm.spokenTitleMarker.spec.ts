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
  expectTitleMarkerAfter,
  microphoneCannotStart,
  mockAudioToTextFailThen,
  mockAudioToTextWithNoSegments,
  placeCaretInTitle,
  speakAndStop,
  speakTheTitle,
  stopSpeaking,
  titleTextPlace,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import {
  dictationMarker,
  expectDictationMarkerAt,
} from "@tests/notes/dictationMarkerTestSupport"
import { titleEditorEl } from "@tests/notes/noteTextContentTestSupport"
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

describe("The pending marker of a title being spoken in New note", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>

  beforeEach(async () => {
    setupNoteNewFormSdkMocks()
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await settleScheduledAutofocus()
  })

  afterEach(() => {
    wrapper.unmount()
  })

  it("sits after the untouched default the words will replace, and is gone when they land", async () => {
    placeCaretInTitle(wrapper, "Unt".length)

    await speakTheTitle(wrapper)
    expect(dictationMarker()!.getAttribute("aria-hidden")).toBe("true")
    expectTitleMarkerAfter(wrapper, "Untitled".length)
    expect(noteTitleText(wrapper)).toBe("Untitled")
    await stopSpeaking(wrapper)

    expect(dictationMarker()).toBeNull()
    expect(noteTitleText(wrapper)).toBe("weekly")
  })

  it("sits after the caret of a typed title", async () => {
    await setNoteNewFormTitle(wrapper, "Project review")
    placeCaretInTitle(wrapper, "Project".length)

    await speakTheTitle(wrapper)

    expectTitleMarkerAfter(wrapper, "Project".length)
  })

  it("sits after the end of a selection in a typed title", async () => {
    await setNoteNewFormTitle(wrapper, "Project review")
    placeCaretInTitle(wrapper, 0, "Project".length)

    await speakTheTitle(wrapper)

    expectTitleMarkerAfter(wrapper, "Project".length)
  })

  it("sits where the first character of a title the author emptied will be drawn, leaving it empty", async () => {
    await setNoteNewFormTitle(wrapper, "A")
    const firstCharacter = titleTextPlace(wrapper, 0)
    await setNoteNewFormTitle(wrapper, "")

    await speakTheTitle(wrapper)

    // The emptied title gives up its line when it turns read-only; the marker follows that on the next frame.
    await expect
      .poll(() => dictationMarker()!.getBoundingClientRect().top)
      .toBeCloseTo(firstCharacter.top, 1)
    expectDictationMarkerAt(firstCharacter)
    expect(titleEditorEl(wrapper).childNodes).toHaveLength(0)
  })

  it("is cleared when nothing was heard", async () => {
    mockAudioToTextWithNoSegments()
    await speakTheTitle(wrapper)
    expect(dictationMarker()).not.toBeNull()

    await stopSpeaking(wrapper)

    expect(dictationMarker()).toBeNull()
  })

  it("is cleared by a failed conversion", async () => {
    mockAudioToTextFailThen("weekly")

    await speakAndStop(wrapper)

    expect(dictationMarker()).toBeNull()
  })

  it("is gone when New note closes while listening", async () => {
    await speakTheTitle(wrapper)
    expect(dictationMarker()).not.toBeNull()

    wrapper.unmount()

    expect(dictationMarker()).toBeNull()
  })

  it("is not shown when the microphone cannot start", async () => {
    microphoneCannotStart()

    await speakTheTitle(wrapper)

    expect(dictationMarker()).toBeNull()
  })
})

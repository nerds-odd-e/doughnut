/// <reference types="@vitest/browser-playwright" />
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "donut-test-fixtures/makeMe"
import { noteTitleText } from "@tests/notes/noteNewFormTestSupport"
import {
  expectTitleMarkerAfter,
  holdSpeakTitleConvertingUntilFinished,
  microphoneCannotStart,
  mockAudioToTextFailThen,
  mockAudioToTextWithNoSegments,
  mountNoteEditableTitle,
  placeCaretInTitle,
  speakAndStop,
  speakTheTitle,
  stopSpeaking,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import { dictationMarker } from "@tests/notes/dictationMarkerTestSupport"
import {
  mockedUpdateTitleCall,
  mockUpdateNoteTitle,
} from "@tests/notes/noteTextContentTestSupport"
import { showToastsOnPage, toastShown } from "@tests/helpers/toastTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { cdp } from "vitest/browser"

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

describe("The pending marker of a title being spoken on an existing note", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>
  const note = makeMe.aNote.title("Orchard notes").please()

  beforeEach(() => {
    mockUpdateNoteTitle()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })
    placeCaretInTitle(wrapper, "Orchard".length)
  })

  afterEach(() => {
    wrapper.unmount()
  })

  it("sits after the caret while listening and through the conversion of Stop, is never title text, and is gone when the words land", async () => {
    expect(dictationMarker()).toBeNull()

    await speakTheTitle(wrapper)
    expect(dictationMarker()!.getAttribute("aria-hidden")).toBe("true")
    expectTitleMarkerAfter(wrapper, "Orchard".length)
    expect(noteTitleText(wrapper)).toBe("Orchard notes")

    const { finishStop } = holdSpeakTitleConvertingUntilFinished()
    await stopSpeaking(wrapper)
    expectTitleMarkerAfter(wrapper, "Orchard".length)
    finishStop()
    await flushPromises()

    expect(dictationMarker()).toBeNull()
    expect(noteTitleText(wrapper)).toBe("Orchard harvest notes")
    vi.advanceTimersByTime(1000)
    await flushPromises()
    expect(mockedUpdateTitleCall).toHaveBeenCalledWith({
      path: { note: note.id },
      body: { newTitle: "Orchard harvest notes" },
    })
  })

  it("sits after the end of a selection the words will replace", async () => {
    placeCaretInTitle(wrapper, 0, "Orchard".length)

    await speakTheTitle(wrapper)

    expectTitleMarkerAfter(wrapper, "Orchard".length)
  })

  it("is cleared when nothing was heard", async () => {
    mockAudioToTextWithNoSegments()
    await speakTheTitle(wrapper)
    expect(dictationMarker()).not.toBeNull()

    await stopSpeaking(wrapper)

    expect(dictationMarker()).toBeNull()
  })

  it("is cleared by a failed conversion", async () => {
    mockAudioToTextFailThen("harvest")

    await speakAndStop(wrapper)

    await toastShown("error")
    expect(dictationMarker()).toBeNull()
  })

  it("is not shown when the microphone cannot start", async () => {
    microphoneCannotStart()

    await speakTheTitle(wrapper)

    await toastShown("error")
    expect(dictationMarker()).toBeNull()
  })

  describe("for an author who prefers reduced motion", () => {
    const prefersReducedMotion = (value: "reduce" | "") =>
      cdp().send("Emulation.setEmulatedMedia", {
        features: [{ name: "prefers-reduced-motion", value }],
      })

    const dotAnimations = () =>
      [...dictationMarker()!.children].map(
        (dot) => getComputedStyle(dot).animationName
      )

    afterEach(() => prefersReducedMotion(""))

    it("has dots that pulse, and stand still under that preference", async () => {
      await speakTheTitle(wrapper)
      expect(dotAnimations()).toEqual(["pulse", "pulse", "pulse"])

      await prefersReducedMotion("reduce")

      expect(dotAnimations()).toEqual(["none", "none", "none"])
    })
  })
})

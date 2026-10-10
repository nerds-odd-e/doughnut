import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "donut-test-fixtures/makeMe"
import { noteTitleText } from "@tests/notes/noteNewFormTestSupport"
import {
  blurAwayFromSpokenTitle,
  expectIdleSpeakTitleButton,
  expectListeningSpeakTitleButton,
  mockAudioToTextFailThen,
  mockAudioToTextWithNoSegments,
  mountNoteEditableTitle,
  placeCaretInTitle,
  selectWholeTitle,
  speakAndStop,
  speakTheTitle,
  speakTitleButton,
  stopSpeaking,
  stubSilentStopRecording,
  titleCaretOffset,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import {
  editTitle,
  mockedUpdateTitleCall,
  mockUpdateNoteTitle,
} from "@tests/notes/noteTextContentTestSupport"
import {
  noToastShown,
  showToastsOnPage,
  toastMessage,
  toastShown,
} from "@tests/helpers/toastTestSupport"
import { referencedTitleSavePanelSelector } from "@tests/notes/textContentWrapperTestSupport"
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

useSpokenTitleTestLifecycle("Apple orchard care")
showToastsOnPage()

describe("NoteEditableTitle spoken title", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>

  beforeEach(() => {
    mockUpdateNoteTitle()
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("is an idle microphone button at the end of the heading line, then a highlighted one while listening", async () => {
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })

    expectIdleSpeakTitleButton(wrapper)
    const heading = wrapper.find("h2").element
    const button = speakTitleButton(wrapper, "Speak the title").element
    expect(heading.nextElementSibling).toBe(button)
    expect(button.getBoundingClientRect().left).toBeGreaterThanOrEqual(
      heading.getBoundingClientRect().right
    )
    expect(button.getBoundingClientRect().top).toBeLessThan(
      heading.getBoundingClientRect().bottom
    )

    await speakTheTitle(wrapper)

    expectListeningSpeakTitleButton(wrapper)
  })

  it("keeps a typed correction after speaking", async () => {
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })
    selectWholeTitle(wrapper)

    await speakAndStop(wrapper)
    await editTitle(wrapper, "Apple orchid care")

    expect(noteTitleText(wrapper)).toBe("Apple orchid care")
  })

  it("does not offer the microphone button when readonly", () => {
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
      readonly: true,
    })

    expect(speakTitleButton(wrapper, "Speak the title").exists()).toBe(false)
    expect(noteTitleText(wrapper)).toBe("Orchard notes")
  })

  it("shows the reference panel for a spoken linked rename and discards when leaving without choosing", async () => {
    const note = makeMe.aNote.title("WikiLinks CI").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
      hasInboundReferences: true,
    })

    selectWholeTitle(wrapper)

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Apple orchard care")
    expect(wrapper.find(referencedTitleSavePanelSelector).exists()).toBe(true)
    expect(mockedUpdateTitleCall).not.toHaveBeenCalled()

    await blurAwayFromSpokenTitle(wrapper)

    expect(noteTitleText(wrapper)).toBe("WikiLinks CI")
    expect(wrapper.find(referencedTitleSavePanelSelector).exists()).toBe(false)
    expect(mockedUpdateTitleCall).not.toHaveBeenCalled()
  })

  it("leaves the title and save alone when nothing was heard", async () => {
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })

    await speakTheTitle(wrapper)
    stubSilentStopRecording()
    await stopSpeaking(wrapper)

    expectIdleSpeakTitleButton(wrapper)
    await noToastShown()
    expect(noteTitleText(wrapper)).toBe("Orchard notes")
    expect(mockedUpdateTitleCall).not.toHaveBeenCalled()
  })

  it("leaves the title and save alone when the response has no segments", async () => {
    mockAudioToTextWithNoSegments()
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })

    await speakAndStop(wrapper)

    expectIdleSpeakTitleButton(wrapper)
    await noToastShown()
    expect(noteTitleText(wrapper)).toBe("Orchard notes")
    expect(mockedUpdateTitleCall).not.toHaveBeenCalled()
  })

  it("leaves the title and the caret alone after a failed conversion, then places the words there on the next speak", async () => {
    mockAudioToTextFailThen("Lighthouse keepers")
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })
    placeCaretInTitle(wrapper, "Orchard".length)

    await speakAndStop(wrapper)

    expect(toastMessage(await toastShown("error"))).toBe(
      "Could not turn your speech into text."
    )
    expectIdleSpeakTitleButton(wrapper)
    expect(noteTitleText(wrapper)).toBe("Orchard notes")
    expect(titleCaretOffset()).toBe("Orchard".length)
    expect(mockedUpdateTitleCall).not.toHaveBeenCalled()

    await speakAndStop(wrapper)
    vi.advanceTimersByTime(1000)
    await flushPromises()

    expect(noteTitleText(wrapper)).toBe("Orchard Lighthouse keepers notes")
    expect(mockedUpdateTitleCall).toHaveBeenCalledWith({
      path: { note: note.id },
      body: { newTitle: "Orchard Lighthouse keepers notes" },
    })
  })
})

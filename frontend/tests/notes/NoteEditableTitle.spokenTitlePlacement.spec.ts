import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "donut-test-fixtures/makeMe"
import { noteTitleText } from "@tests/notes/noteNewFormTestSupport"
import {
  blurAwayFromSpokenTitle,
  hearing,
  mountNoteEditableTitle,
  placeCaretInTitle,
  selectWholeTitle,
  speakAndStop,
  titleCaretOffset,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import {
  mockedUpdateTitleCall,
  mockUpdateNoteTitle,
  titleEditorEl,
} from "@tests/notes/noteTextContentTestSupport"
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

describe("NoteEditableTitle spoken title placement", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>

  beforeEach(() => {
    mockUpdateNoteTitle()
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("appends at the caret at the end", async () => {
    hearing("notes")
    const note = makeMe.aNote.title("Orchard").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })
    placeCaretInTitle(wrapper, 7)

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Orchard notes")
    expect(document.activeElement).toBe(titleEditorEl(wrapper))
    expect(titleCaretOffset()).toBe("Orchard notes".length)

    vi.advanceTimersByTime(1000)
    await flushPromises()

    expect(mockedUpdateTitleCall).toHaveBeenCalledWith({
      path: { note: note.id },
      body: { newTitle: "Orchard notes" },
    })
  })

  it("replaces the selection", async () => {
    hearing("Garden")
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })
    placeCaretInTitle(wrapper, 0, 7)

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Garden notes")
    expect(titleCaretOffset()).toBe("Garden".length)
  })

  it("inserts between words", async () => {
    hearing("harvest")
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })
    placeCaretInTitle(wrapper, 7)

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Orchard harvest notes")
    expect(titleCaretOffset()).toBe("Orchard harvest".length)
  })

  it("appends when the author never placed a caret", async () => {
    hearing("today")
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Orchard notes today")
  })

  it("appends to another note's title after a caret was placed in the earlier one", async () => {
    hearing("today")
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })
    placeCaretInTitle(wrapper, 7)
    await blurAwayFromSpokenTitle(wrapper)
    const anotherNote = makeMe.aNote.title("Pear tree").please()
    await wrapper.setProps({
      noteTopology: anotherNote.noteTopology,
      noteId: anotherNote.id,
    })

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Pear tree today")
  })

  it("replaces the whole selected title", async () => {
    hearing("Pear orchard care")
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })
    selectWholeTitle(wrapper)

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Pear orchard care")
  })

  it("replaces the selected title again when speaking a second time", async () => {
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })
    selectWholeTitle(wrapper)
    await speakAndStop(wrapper)

    hearing("Pear orchard care")
    selectWholeTitle(wrapper)
    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Pear orchard care")
  })
})

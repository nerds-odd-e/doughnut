import { AiAudioController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { ComponentPublicInstance } from "vue"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { settleScheduledAutofocus } from "@tests/helpers/focusTargetTestSupport"
import { audioTextResponse } from "@tests/notes/noteAudioToolsTestSupport"
import { noteTitleText } from "@tests/notes/noteNewFormTestSupport"
import {
  blurAwayFromSpokenTitle,
  findSpeakTitleButtonByText,
  mountNoteEditableTitle,
  speakAndStop,
  speakTheTitle,
  speakTitleStatus,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import {
  editTitle,
  mockedUpdateTitleCall,
  mockUpdateNoteTitle,
} from "@tests/notes/noteTextContentTestSupport"
import { referencedTitleSavePanelSelector } from "@tests/notes/textContentWrapperTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

vi.mock("@/models/audio/recorderWorklet", async () => {
  const { recorderWorkletMockExports } = await import(
    "@tests/notes/noteAudioToolsMocks"
  )
  return recorderWorkletMockExports()
})

vi.mock("@/models/audio/audioRecorder", async () => {
  const { audioRecorderInvokingCallbackMockExports } = await import(
    "@tests/notes/noteAudioToolsMocks"
  )
  return audioRecorderInvokingCallbackMockExports()
})

useSpokenTitleTestLifecycle("Apple orchard care")

describe("NoteEditableTitle spoken title", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>

  beforeEach(() => {
    mockUpdateNoteTitle()
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("names the control Stop and shows recording status while listening", async () => {
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })

    expect(findSpeakTitleButtonByText(wrapper, "Speak the title")).toBeTruthy()
    expect(findSpeakTitleButtonByText(wrapper, "Stop")).toBeUndefined()
    expect(speakTitleStatus(wrapper)).toBeUndefined()

    await speakTheTitle(wrapper)

    expect(findSpeakTitleButtonByText(wrapper, "Stop")).toBeTruthy()
    expect(
      findSpeakTitleButtonByText(wrapper, "Speak the title")
    ).toBeUndefined()
    expect(speakTitleStatus(wrapper)).toBe("Recording. Speak now.")
  })

  it("replaces an existing title with the heard words and focuses the title", async () => {
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })

    await speakAndStop(wrapper)
    await settleScheduledAutofocus()

    expect(noteTitleText(wrapper)).toBe("Apple orchard care")
    expect(document.activeElement).toBe(
      wrapper.find('[data-test="note-title"]').element
    )

    vi.advanceTimersByTime(1000)
    await flushPromises()

    expect(mockedUpdateTitleCall).toHaveBeenCalledWith({
      path: { note: note.id },
      body: { newTitle: "Apple orchard care" },
    })
  })

  it("keeps a typed correction after speaking", async () => {
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })

    await speakAndStop(wrapper)
    await editTitle(wrapper, "Apple orchid care")

    expect(noteTitleText(wrapper)).toBe("Apple orchid care")
  })

  it("replaces the title again when speaking a second time", async () => {
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
    })

    await speakAndStop(wrapper)
    expect(noteTitleText(wrapper)).toBe("Apple orchard care")

    mockSdkService(
      AiAudioController,
      "audioToText",
      audioTextResponse("Pear orchard care")
    )
    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Pear orchard care")
  })

  it("does not offer Speak the title when readonly", () => {
    const note = makeMe.aNote.title("Orchard notes").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
      readonly: true,
    })

    expect(
      findSpeakTitleButtonByText(wrapper, "Speak the title")
    ).toBeUndefined()
    expect(noteTitleText(wrapper)).toBe("Orchard notes")
  })

  it("shows the reference panel for a spoken linked rename and discards when leaving without choosing", async () => {
    const note = makeMe.aNote.title("WikiLinks CI").please()
    wrapper = mountNoteEditableTitle({
      noteTopology: note.noteTopology,
      noteId: note.id,
      hasInboundReferences: true,
    })

    await speakAndStop(wrapper)
    await settleScheduledAutofocus()

    expect(noteTitleText(wrapper)).toBe("Apple orchard care")
    expect(wrapper.find(referencedTitleSavePanelSelector).exists()).toBe(true)
    expect(mockedUpdateTitleCall).not.toHaveBeenCalled()

    await blurAwayFromSpokenTitle(wrapper)

    expect(noteTitleText(wrapper)).toBe("WikiLinks CI")
    expect(wrapper.find(referencedTitleSavePanelSelector).exists()).toBe(false)
    expect(mockedUpdateTitleCall).not.toHaveBeenCalled()
  })
})

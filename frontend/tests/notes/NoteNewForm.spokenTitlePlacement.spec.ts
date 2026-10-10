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
  hearing,
  placeCaretInTitle,
  speakAndStop,
  titleCaretOffset,
  useSpokenTitleTestLifecycle,
} from "@tests/notes/spokenTitleTestSupport"
import { settleScheduledAutofocus } from "@tests/helpers/focusTargetTestSupport"
import { titleEditorEl } from "@tests/notes/noteTextContentTestSupport"
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

describe("NoteNewForm spoken title placement", () => {
  let wrapper: VueWrapper<ComponentPublicInstance>

  beforeEach(() => {
    setupNoteNewFormSdkMocks()
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("replaces an untouched default title", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await settleScheduledAutofocus()

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Photosynthesis in desert plants.")
    expect(document.activeElement).toBe(titleEditorEl(wrapper))
    expect(titleCaretOffset()).toBe("Photosynthesis in desert plants.".length)
  })

  it("replaces an untouched default title after the selection was lost", async () => {
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await settleScheduledAutofocus()
    window.getSelection()!.removeAllRanges()
    titleEditorEl(wrapper).blur()

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Photosynthesis in desert plants.")
    expect(document.activeElement).toBe(titleEditorEl(wrapper))
    expect(titleCaretOffset()).toBe("Photosynthesis in desert plants.".length)
  })

  it("joins heard segments onto a title pattern", async () => {
    hearing("weekly review")
    wrapper = mountNoteNewForm(
      { ...notebookRootProps, initialTitle: "2026-10-06" },
      { attachTo: document.body }
    )
    await settleScheduledAutofocus()

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("2026-10-06 weekly review")
  })

  it("joins onto a typed title", async () => {
    hearing("weekly review")
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await settleScheduledAutofocus()
    await setNoteNewFormTitle(wrapper, "Project")
    placeCaretInTitle(wrapper, "Project".length)

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Project weekly review")
  })

  it("inserts at a caret inside a typed title", async () => {
    hearing("weekly")
    wrapper = mountNoteNewForm(notebookRootProps, { attachTo: document.body })
    await settleScheduledAutofocus()
    await setNoteNewFormTitle(wrapper, "Project review")
    placeCaretInTitle(wrapper, "Project".length)

    await speakAndStop(wrapper)

    expect(noteTitleText(wrapper)).toBe("Project weekly review")
    expect(document.activeElement).toBe(titleEditorEl(wrapper))
    expect(titleCaretOffset()).toBe("Project weekly".length)
  })
})

import { useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import { advanceNoteContentSaveDebounce } from "@tests/helpers/noteContentDebounceTestSupport"
import { holdNoteContentSave } from "@tests/notes/noteTextContentTestSupport"
import { useNoteAudioToolsTestLifecycle } from "@tests/notes/noteAudioToolsTestSupport"
import {
  dictatedPassage as passage,
  useBodyEditorWithHeldDictation,
} from "@tests/notes/noteAudioToolsTypingTestSupport"
import {
  blurTextarea,
  richQuillInstance,
  setTextareaValue,
  textareaEl,
} from "@tests/notes/noteEditableContentTestSupport"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"

vi.mock("@/models/audio/recorderWorklet", async () => {
  const { recorderWorkletMockExports } = await import(
    "@tests/notes/noteAudioToolsMocks"
  )
  return recorderWorkletMockExports()
})

vi.mock("@/models/audio/audioRecorder", async () => {
  const { audioRecorderMockExports } = await import(
    "@tests/notes/noteAudioToolsMocks"
  )
  return audioRecorderMockExports()
})

vi.mock("@/models/wakeLocker", async () => {
  const { wakeLockerMockExports } = await import(
    "@tests/notes/noteAudioToolsMocks"
  )
  return wakeLockerMockExports()
})

useNoteAudioToolsTestLifecycle()

describe("NoteAudioTools while the author types in the open body editor", () => {
  const noteStore = useNoteStore()
  const {
    mountEditorAndAudioTools,
    showInEditor,
    dictate,
    whileAudioIsPending,
    savedContents,
    lastSavedContent,
  } = useBodyEditorWithHeldDictation()

  const redBicycleBody = [
    "Original paragraph one: The museum opens at nine each morning. Our tickets are booked for Tuesday.",
    "Original paragraph two: The blue notebook contains the garden measurements. Keep the oak tree map beside it.",
    "The lighthouse keeper painted the front door bright yellow. Tomorrow we will bring fresh oranges to the beach.",
  ].join("\n\n")
  const typed = " MANUAL EDIT: Keep this red bicycle sentence."
  const giftBody =
    "The book I bought yesterday is a gift from my sister. She enjoys gardens."
  const correctedGiftBody = giftBody.replace("from my sister", "for my sister")

  it("keeps typing at the end of the rich editor and saves the passage after it", async () => {
    const { wrapper } = mountEditorAndAudioTools(redBicycleBody, false)
    const quill = () => richQuillInstance(wrapper)

    await whileAudioIsPending(async () => {
      quill().insertText(quill().getLength() - 1, typed, "user")
      await flushPromises()
    })

    expect(quill().getText()).toContain(`${typed}${passage}\n`)
    expect(lastSavedContent()).toBe(`${redBicycleBody}${typed}${passage}`)
  })

  it("keeps typing at the end of the Markdown editor and saves the passage after it", async () => {
    const { wrapper } = mountEditorAndAudioTools(redBicycleBody, true)

    await whileAudioIsPending(async () => {
      await setTextareaValue(wrapper, `${redBicycleBody}${typed}`)
    })

    expect(textareaEl(wrapper).value).toBe(
      `${redBicycleBody}${typed}${passage}`
    )
    expect(lastSavedContent()).toBe(`${redBicycleBody}${typed}${passage}`)
  })

  it("keeps a correction in the middle and puts the passage at the end", async () => {
    const { wrapper } = mountEditorAndAudioTools(giftBody, false)
    const quill = () => richQuillInstance(wrapper)

    await whileAudioIsPending(async () => {
      const at = quill().getText().indexOf("from my sister")
      quill().deleteText(at, "from".length, "user")
      quill().insertText(at, "for", "user")
      await flushPromises()
    })

    expect(lastSavedContent()).toBe(`${correctedGiftBody}${passage}`)
  })

  it("keeps the Markdown editor's caret where the author was typing", async () => {
    const caret = correctedGiftBody.indexOf("for my sister") + "for".length
    const { wrapper } = mountEditorAndAudioTools(giftBody, true)

    await whileAudioIsPending(async () => {
      textareaEl(wrapper).focus()
      const el = await setTextareaValue(wrapper, correctedGiftBody)
      el.setSelectionRange(caret, caret)
    })

    const el = textareaEl(wrapper)
    expect(el.value).toBe(`${correctedGiftBody}${passage}`)
    expect([el.selectionStart, el.selectionEnd]).toEqual([caret, caret])
  })

  it("saves the passage as soon as it joins the open editor's draft", async () => {
    mountEditorAndAudioTools(redBicycleBody, true)

    await dictate()

    expect(lastSavedContent()).toBe(`${redBicycleBody}${passage}`)
  })

  it("keeps typing that was already saved and saves the passage after it once", async () => {
    const { wrapper } = mountEditorAndAudioTools(redBicycleBody, true)

    await whileAudioIsPending(async () => {
      await setTextareaValue(wrapper, `${redBicycleBody}${typed}`)
      await advanceNoteContentSaveDebounce()
      expect(lastSavedContent()).toBe(`${redBicycleBody}${typed}`)
    })

    expect(textareaEl(wrapper).value).toBe(
      `${redBicycleBody}${typed}${passage}`
    )
    expect(lastSavedContent()).toBe(`${redBicycleBody}${typed}${passage}`)
  })

  it("keeps typing whose save is still in flight and saves the passage after it once", async () => {
    const { wrapper, note } = mountEditorAndAudioTools(redBicycleBody, true)
    const releaseSave = holdNoteContentSave((saved) =>
      makeMe.aNoteRealm.id(note.id).content(saved).please()
    )

    await whileAudioIsPending(async () => {
      await setTextareaValue(wrapper, `${redBicycleBody}${typed}`)
      await advanceNoteContentSaveDebounce()
    })
    releaseSave()
    await flushPromises()

    expect(lastSavedContent()).toBe(`${redBicycleBody}${typed}${passage}`)
  })

  it("adds the passage to the saved body of a note the author has left", async () => {
    const { wrapper, note: noteA } = mountEditorAndAudioTools(
      redBicycleBody,
      true
    )
    const noteB = makeMe.aNoteRealm.content("Note B body.").please()

    await whileAudioIsPending(async () => {
      await setTextareaValue(wrapper, `${redBicycleBody}${typed}`)
      await blurTextarea(wrapper)
      await showInEditor(noteB)
    })

    expect(lastSavedContent(noteA.id)).toBe(
      `${redBicycleBody}${typed}${passage}`
    )
    expect(noteStore.refOfNoteRealm(noteA.id).value?.note.content).toBe(
      `${redBicycleBody}${typed}${passage}`
    )
    expect(textareaEl(wrapper).value).toBe("Note B body.")
    expect(savedContents(noteB.id)).toHaveLength(0)
  })
})

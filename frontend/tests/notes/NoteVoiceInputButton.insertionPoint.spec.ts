import { useBodyEditorWithVoiceInput } from "@tests/notes/noteVoiceInputBodyEditorTestSupport"
import { useNoteVoiceInputTestLifecycle } from "@tests/notes/noteVoiceInputButtonTestSupport"
import {
  richQuillEditorEl,
  richQuillInstance,
  textareaEl,
} from "@tests/notes/noteEditableContentTestSupport"
import { closeAndFlushNoteContentMutations } from "@/composables/noteContentMutationBarrier"
import { useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, vi } from "vitest"

vi.mock("@/models/audio/recorderWorklet", async () => {
  const { recorderWorkletMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return recorderWorkletMockExports()
})

vi.mock("@/models/audio/audioRecorder", async () => {
  const { audioRecorderMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return audioRecorderMockExports()
})

vi.mock("@/models/wakeLocker", async () => {
  const { wakeLockerMockExports } = await import(
    "@tests/notes/noteVoiceInputButtonMocks"
  )
  return wakeLockerMockExports()
})

useNoteVoiceInputTestLifecycle()

const body = "The orchard gate is green. The well is deep."
const afterGreen = "The orchard gate is green.".length
const passage = "The orchard path leads down to the river."

describe("Voice input into the open rich body editor", () => {
  const {
    mountEditorAndVoiceInput,
    mountRichEditorWithSelection,
    hears,
    start,
    passageArrives,
    stop,
    editAsMarkdown,
    lastSavedContent,
  } = useBodyEditorWithVoiceInput()

  it("puts each passage at the caret after the one before and saves them there", async () => {
    const wrapper = await mountRichEditorWithSelection(body, afterGreen)

    await start()
    await passageArrives("The hinge creaks.")
    await passageArrives("The latch is new.")
    hears("The path is short.")
    await stop()

    const dictated =
      "The orchard gate is green. The hinge creaks. The latch is new. The path is short. The well is deep."
    expect(richQuillInstance(wrapper).getText()).toBe(`${dictated}\n`)
    expect(lastSavedContent()).toBe(dictated)
  })

  it("replaces the selection, spacing the words from both neighbours", async () => {
    const wrapper = await mountRichEditorWithSelection(
      body,
      "The orchard".length,
      " gate ".length
    )

    await start()
    hears("door")
    await stop()

    expect(richQuillInstance(wrapper).getText()).toBe(
      "The orchard door is green. The well is deep.\n"
    )
  })

  it("joins the end when the author has placed no caret", async () => {
    mountEditorAndVoiceInput(body, false)

    await start()
    hears("The path is short.")
    await stop()

    expect(lastSavedContent()).toBe(`${body} The path is short.`)
  })

  it("joins the end of a body the rich editor cannot keep", async () => {
    const unkept = "- [ ] oil the hinge\n- [x] paint the gate"
    mountEditorAndVoiceInput(unkept, false)
    await flushPromises()

    await start()
    await passageArrives("The path is short.")

    expect(lastSavedContent()).toBe(`${unkept} The path is short.`)
  })

  it("goes on at the end of the Markdown editor the author switches to", async () => {
    const wrapper = await mountRichEditorWithSelection(body, afterGreen)
    await start()

    await editAsMarkdown(true)
    expect(textareaEl(wrapper).readOnly).toBe(true)
    await passageArrives("The path is short.")

    expect(textareaEl(wrapper).value).toBe(`${body} The path is short.`)
  })
})

describe("Voice input into the open Markdown body editor", () => {
  const {
    mountEditorAndVoiceInput,
    mountMarkdownEditorWithSelection,
    hears,
    start,
    passageArrives,
    stop,
    lastSavedContent,
  } = useBodyEditorWithVoiceInput()

  it("replaces the selection with Japanese words without spaces", async () => {
    const el = await mountMarkdownEditorWithSelection(
      "鐘は毎時間鳴ります。",
      "鐘は".length,
      "鐘は毎時間".length
    )

    await start()
    hears("正午に")
    await stop()

    expect(el.value).toBe("鐘は正午に鳴ります。")
  })

  it("saves the passage as soon as it joins the open editor", async () => {
    mountEditorAndVoiceInput(body, true)
    await start()

    await passageArrives(passage)

    expect(lastSavedContent()).toBe(`${body} ${passage}`)
  })

  it("joins a Japanese passage to the open editor without a space", async () => {
    mountEditorAndVoiceInput("鐘は毎時間鳴ります。", true)
    await start()

    await passageArrives("果樹園は古いです。")

    expect(lastSavedContent()).toBe("鐘は毎時間鳴ります。果樹園は古いです。")
  })
})

describe("Voice input that the open body editor does not take", () => {
  const noteStore = useNoteStore()
  const {
    mountEditorAndVoiceInput,
    hears,
    holdAudio,
    start,
    stop,
    showInEditor,
    savedContents,
    lastSavedContent,
  } = useBodyEditorWithVoiceInput()

  it("adds the passage to the saved body while a save-then-change pause holds the editor", async () => {
    const { note } = mountEditorAndVoiceInput(body, true)
    await start()
    const releaseAudio = holdAudio()
    hears(passage)

    await stop()
    await closeAndFlushNoteContentMutations(note.id)
    await releaseAudio()

    expect(lastSavedContent()).toBe(`${body} ${passage}`)
  })

  it("adds the passage to the saved body of a note the author has left", async () => {
    const { wrapper, note: noteA } = mountEditorAndVoiceInput(body, true)
    const noteB = makeMe.aNoteRealm.content("Note B body.").please()
    await start()
    const releaseAudio = holdAudio()
    hears(passage)

    await showInEditor(noteB)
    await releaseAudio()

    expect(lastSavedContent(noteA.id)).toBe(`${body} ${passage}`)
    expect(noteStore.refOfNoteRealm(noteA.id).value?.note.content).toBe(
      `${body} ${passage}`
    )
    expect(textareaEl(wrapper).value).toBe("Note B body.")
    expect(textareaEl(wrapper).readOnly).toBe(false)
    expect(savedContents(noteB.id)).toHaveLength(0)
  })

  it("leaves the rich editor editable on the note the author moved to, saving the remainder to the note left", async () => {
    const { wrapper, note: noteA } = mountEditorAndVoiceInput(body, false)
    const noteB = makeMe.aNoteRealm.content("Note B body.").please()
    await start()
    const releaseAudio = holdAudio()
    hears(passage)

    await showInEditor(noteB)
    await releaseAudio()

    expect(richQuillEditorEl(wrapper).getAttribute("contenteditable")).toBe(
      "true"
    )
    expect(richQuillInstance(wrapper).getText()).toBe("Note B body.\n")
    expect(lastSavedContent(noteA.id)).toBe(`${body} ${passage}`)
  })
})

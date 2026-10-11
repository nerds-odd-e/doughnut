import { useBodyEditorWithVoiceInput } from "@tests/notes/noteVoiceInputBodyEditorTestSupport"
import { useNoteVoiceInputTestLifecycle } from "@tests/notes/noteVoiceInputButtonTestSupport"
import { richQuillInstance } from "@tests/notes/noteEditableContentTestSupport"
import { showToastsOnPage, toastShown } from "@tests/helpers/toastTestSupport"
import makeMe from "donut-test-fixtures/makeMe"
import type { VueWrapper } from "@vue/test-utils"
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
showToastsOnPage()

const body = "The orchard gate is green. The well is deep."
const afterGreen = "The orchard gate is green.".length

const marker = () =>
  document.querySelector<HTMLElement>('[data-testid="dictation-marker"]')

describe("The pending marker of body voice input", () => {
  const {
    mountRichEditorWithSelection,
    mountMarkdownEditorWithSelection,
    hears,
    holdAudio,
    conversionFailsKeepingTheRecording,
    microphoneCannotStart,
    start,
    passageArrives,
    stop,
    retry,
    showInEditor,
    lastSavedContent,
  } = useBodyEditorWithVoiceInput()

  /** Where the text position `index` is drawn on the page, as `[left, top, height]`. */
  const drawnAt = (wrapper: VueWrapper, index: number) => {
    const quill = richQuillInstance(wrapper)
    const bounds = quill.getBounds(index)!
    const container = quill.container.getBoundingClientRect()
    return [
      container.left + bounds.left,
      container.top + bounds.top,
      bounds.height,
    ]
  }

  const markerDrawnAt = () => {
    const rect = marker()!.getBoundingClientRect()
    return [rect.left, rect.top, rect.height]
  }

  const expectMarkerAt = (wrapper: VueWrapper, index: number) => {
    const expected = drawnAt(wrapper, index)
    markerDrawnAt().forEach((actual, i) => {
      expect(actual).toBeCloseTo(expected[i]!, 1)
    })
  }

  it("sits after the caret, follows each passage, stays through the conversion of Stop, and is never content", async () => {
    const wrapper = await mountRichEditorWithSelection(body, afterGreen)
    expect(marker()).toBeNull()

    await start()
    expect(marker()!.getAttribute("aria-hidden")).toBe("true")
    expectMarkerAt(wrapper, afterGreen)

    await passageArrives("The hinge creaks.")
    const afterCreaks = "The orchard gate is green. The hinge creaks.".length
    expectMarkerAt(wrapper, afterCreaks)

    const releaseAudio = holdAudio()
    hears("It needs oil.")
    await stop()
    expectMarkerAt(wrapper, afterCreaks)
    await releaseAudio()

    expect(marker()).toBeNull()
    const text =
      "The orchard gate is green. The hinge creaks. It needs oil. The well is deep."
    expect(richQuillInstance(wrapper).getText()).toBe(`${text}\n`)
    expect(lastSavedContent()).toBe(text)
  })

  it("sits after the end of a selection the words will replace", async () => {
    const wrapper = await mountRichEditorWithSelection(
      body,
      4,
      "orchard".length
    )

    await start()

    expectMarkerAt(wrapper, 4 + "orchard".length)
  })

  it("is cleared when nothing was heard", async () => {
    await mountRichEditorWithSelection(body, afterGreen)
    await start()
    hears([])

    await stop()

    expect(marker()).toBeNull()
  })

  it("is not shown when the microphone cannot start", async () => {
    await mountRichEditorWithSelection(body, afterGreen)
    microphoneCannotStart()

    await start()

    await toastShown("error")
    expect(marker()).toBeNull()
  })

  it("is cleared by a failed conversion, and sits at the caret of the retry click until the retry's text arrives", async () => {
    const wrapper = await mountRichEditorWithSelection(body, afterGreen)
    await start()
    conversionFailsKeepingTheRecording()
    await stop()
    expect(marker()).toBeNull()

    richQuillInstance(wrapper).setSelection(3, 0, "user")
    const releaseAudio = holdAudio()
    hears("The path is short.")
    await retry()
    expectMarkerAt(wrapper, 3)
    await releaseAudio()

    expect(marker()).toBeNull()
  })

  it("is cleared when the author leaves the note", async () => {
    await mountRichEditorWithSelection(body, afterGreen)
    await start()
    expect(marker()).not.toBeNull()

    await showInEditor(makeMe.aNoteRealm.content("Another note.").please())

    expect(marker()).toBeNull()
  })

  it("is not shown in the Markdown editor", async () => {
    const el = await mountMarkdownEditorWithSelection(body, afterGreen)

    await start()

    expect(el.readOnly).toBe(true)
    expect(marker()).toBeNull()
  })
})

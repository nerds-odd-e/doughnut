import { useBodyEditorWithVoiceInput } from "@tests/notes/noteVoiceInputBodyEditorTestSupport"
import { useNoteVoiceInputTestLifecycle } from "@tests/notes/noteVoiceInputButtonTestSupport"
import {
  richQuillEditorEl,
  richQuillInstance,
} from "@tests/notes/noteEditableContentTestSupport"
import {
  noToastShown,
  showToastsOnPage,
  toastMessage,
  toastShown,
} from "@tests/helpers/toastTestSupport"
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

describe("The open rich body editor during voice input", () => {
  const {
    mountRichEditorWithSelection,
    hears,
    holdAudio,
    conversionFailsKeepingTheRecording,
    microphoneCannotStart,
    start,
    passageArrives,
    stop,
    retry,
    savedContents,
  } = useBodyEditorWithVoiceInput()

  const editable = (wrapper: VueWrapper) =>
    richQuillEditorEl(wrapper).getAttribute("contenteditable")

  it("is read-only while the session runs and keeps its target when the selection moves", async () => {
    const wrapper = await mountRichEditorWithSelection(body, afterGreen)

    await start()
    expect(editable(wrapper)).toBe("false")
    richQuillInstance(wrapper).setSelection(0, 0, "user")
    await passageArrives("The hinge creaks.")

    expect(richQuillInstance(wrapper).getText()).toBe(
      "The orchard gate is green. The hinge creaks. The well is deep.\n"
    )
  })

  it("stays read-only until the text of Stop has arrived, then is editable with the caret after the words", async () => {
    const wrapper = await mountRichEditorWithSelection(body, afterGreen)
    await start()
    const releaseAudio = holdAudio()
    hears("The hinge creaks.")

    await stop()
    expect(editable(wrapper)).toBe("false")
    await releaseAudio()

    expect(editable(wrapper)).toBe("true")
    expect(richQuillInstance(wrapper).getSelection()).toEqual({
      index: "The orchard gate is green. The hinge creaks.".length,
      length: 0,
    })
  })

  it("stays editable beside the microphone toast when recording cannot start", async () => {
    const wrapper = await mountRichEditorWithSelection(body, afterGreen)
    microphoneCannotStart()

    await start()

    expect(toastMessage(await toastShown("error"))).toContain(
      "Could not use the microphone."
    )
    expect(editable(wrapper)).toBe("true")
  })

  it("is editable again and unchanged, with no message, when nothing was heard", async () => {
    const wrapper = await mountRichEditorWithSelection(body, afterGreen)
    await start()
    hears([])

    await stop()

    expect(editable(wrapper)).toBe("true")
    expect(richQuillInstance(wrapper).getText()).toBe(`${body}\n`)
    expect(savedContents()).toHaveLength(0)
    await noToastShown()
  })

  describe("after a failed conversion at Stop", () => {
    async function failAtStop() {
      const wrapper = await mountRichEditorWithSelection(body, afterGreen)
      await start()
      conversionFailsKeepingTheRecording()
      await stop()
      return wrapper
    }

    it("is editable again beside the error toast", async () => {
      const wrapper = await failAtStop()

      expect(toastMessage(await toastShown("error"))).toContain(
        "Could not turn your speech into text."
      )
      expect(editable(wrapper)).toBe("true")
    })

    it("is read-only during the retry, whose text lands at the caret of the retry click", async () => {
      const wrapper = await failAtStop()
      richQuillInstance(wrapper).setSelection(0, 0, "user")
      const releaseAudio = holdAudio()
      hears("The path is short.")

      await retry()
      expect(editable(wrapper)).toBe("false")
      await releaseAudio()

      expect(richQuillInstance(wrapper).getText()).toBe(
        `The path is short. ${body}\n`
      )
    })
  })
})

describe("The open Markdown body editor during voice input", () => {
  const { mountMarkdownEditorWithSelection, hears, holdAudio, start, stop } =
    useBodyEditorWithVoiceInput()

  it("is read-only until the text has arrived at the caret, then is editable with the caret after the words", async () => {
    const el = await mountMarkdownEditorWithSelection(body, afterGreen)
    await start()
    const releaseAudio = holdAudio()
    hears("The hinge creaks.")

    await stop()
    expect(el.readOnly).toBe(true)
    await releaseAudio()

    expect(el.value).toBe(
      "The orchard gate is green. The hinge creaks. The well is deep."
    )
    expect(el.readOnly).toBe(false)
    const caret = "The orchard gate is green. The hinge creaks.".length
    expect([el.selectionStart, el.selectionEnd]).toEqual([caret, caret])
  })
})

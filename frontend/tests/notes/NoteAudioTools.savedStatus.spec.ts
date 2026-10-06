import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import NoteAudioTools from "@/components/notes/widgets/NoteAudioTools.vue"
import { useNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { advanceNoteContentSaveDebounce } from "@tests/helpers/noteContentDebounceTestSupport"
import {
  audioTextResponse,
  audioToolsVm,
  dictationStatus,
  mountNoteAudioTools,
  processAudio,
  startRecording,
  stopRecording,
  useNoteAudioToolsTestLifecycle,
  type NoteAudioToolsWrapper,
} from "@tests/notes/noteAudioToolsTestSupport"
import {
  dictatedPassage as passage,
  useBodyEditorWithHeldDictation,
} from "@tests/notes/noteAudioToolsTypingTestSupport"
import {
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

const stopConvertsOnePassage = (tools: NoteAudioToolsWrapper) => {
  mockSdkService(AiAudioController, "audioToText", audioTextResponse(passage))
  audioToolsVm(tools).audioRecorder.stopRecording.mockImplementation(
    async () => {
      await processAudio(tools)
      return new File([], "test.webm")
    }
  )
}

/** Holds each body save until `finishSave` settles it, oldest first. */
const holdSaves = () => {
  const outcomes: ((succeeded: boolean) => void)[] = []
  const held: Promise<boolean>[] = []
  const nth = (i: number) => {
    held[i] ??= new Promise<boolean>((resolve) => (outcomes[i] = resolve))
    return held[i]!
  }
  let calls = 0
  let finished = 0
  vi.spyOn(TextContentController, "updateNoteContent").mockImplementation(
    async (options) =>
      (await nth(calls++))
        ? wrapSdkResponse(
            makeMe.aNoteRealm
              .id(options.path.note)
              .content(options.body?.content ?? "")
              .please()
          )
        : wrapSdkError({ message: "save failed" })
  )
  return (succeeded: boolean) => {
    nth(finished)
    outcomes[finished++]!(succeeded)
  }
}

describe("NoteAudioTools status after Stop with the body editor open", () => {
  const { mountEditorAndAudioTools } = useBodyEditorWithHeldDictation()
  const body = "The museum opens at nine."

  const stopWithSaveHeld = async () => {
    const { wrapper } = mountEditorAndAudioTools(body, true)
    const tools = wrapper.findComponent(NoteAudioTools) as NoteAudioToolsWrapper
    await startRecording(tools)
    stopConvertsOnePassage(tools)
    const finishSave = holdSaves()
    await stopRecording(tools)
    return { wrapper, tools, finishSave }
  }

  it("shows the passage at once but says added only after its save", async () => {
    const { wrapper, tools, finishSave } = await stopWithSaveHeld()

    expect(textareaEl(wrapper).value).toBe(`${body} ${passage}`)
    expect(dictationStatus(tools)).toBe("Turning your speech into text…")

    finishSave(true)
    await flushPromises()
    expect(dictationStatus(tools)).toBe("Added to your note.")
  })

  it("claims neither added nor no speech when the save fails", async () => {
    const { tools, finishSave } = await stopWithSaveHeld()

    finishSave(false)
    await flushPromises()
    expect(dictationStatus(tools)).toBe("Ready to record")
  })

  it("says added once the passage is saved although the author typed after it joined", async () => {
    const { wrapper, tools, finishSave } = await stopWithSaveHeld()

    await setTextareaValue(wrapper, `${body} ${passage} More typing.`)
    finishSave(true)
    await flushPromises()
    expect(dictationStatus(tools)).toBe("Added to your note.")
  })

  describe("when typing replaces the passage's draft before it is sent", () => {
    const typed = " Typed first."
    const typedAfter = " Typed after."

    const passageDraftReplacedByTyping = async () => {
      const { wrapper } = mountEditorAndAudioTools(body, true)
      const tools = wrapper.findComponent(
        NoteAudioTools
      ) as NoteAudioToolsWrapper
      const finishSave = holdSaves()
      await startRecording(tools)
      await setTextareaValue(wrapper, `${body}${typed}`)
      await advanceNoteContentSaveDebounce()
      stopConvertsOnePassage(tools)
      await stopRecording(tools)
      await setTextareaValue(wrapper, `${body}${typed} ${passage}${typedAfter}`)
      finishSave(true)
      await flushPromises()
      return { tools, finishSave }
    }

    it("waits for the newer draft holding the passage and claims nothing when its save fails", async () => {
      const { tools, finishSave } = await passageDraftReplacedByTyping()
      expect(dictationStatus(tools)).toBe("Turning your speech into text…")

      finishSave(false)
      await flushPromises()
      expect(dictationStatus(tools)).toBe("Ready to record")
    })

    it("says added once the newer draft holding the passage is saved", async () => {
      const { tools, finishSave } = await passageDraftReplacedByTyping()

      finishSave(true)
      await flushPromises()
      expect(dictationStatus(tools)).toBe("Added to your note.")
    })
  })
})

describe("NoteAudioTools status after Stop without a body editor", () => {
  it("claims neither added nor no speech when the save fails", async () => {
    const realm = makeMe.aNoteRealm.please()
    useNoteStore().refreshNoteRealm(realm)
    const tools = mountNoteAudioTools(realm.note)
    await startRecording(tools)
    stopConvertsOnePassage(tools)
    holdSaves()(false)

    await stopRecording(tools)
    expect(dictationStatus(tools)).toBe("Ready to record")
    tools.unmount()
  })
})

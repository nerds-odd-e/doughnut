import {
  AiAudioController,
  AiController,
  NoteController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import { useNoteStore } from "@/store/noteStore"
import {
  midSpeechChunk,
  dictatedTextResponse,
  mountNoteAudioTools,
  processAudio,
  useNoteAudioToolsTestLifecycle,
  type NoteAudioToolsWrapper,
} from "@tests/notes/noteAudioToolsTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

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

describe("NoteAudioTools audio processing", () => {
  let wrapper: NoteAudioToolsWrapper
  let audioToTextMock: ReturnType<typeof mockSdkService>
  let updateContentMock: ReturnType<typeof mockSdkService>
  const originalRealm = makeMe.aNoteRealm.content("Original body.").please()
  const note = originalRealm.note
  const noteStore = useNoteStore()
  beforeEach(() => {
    audioToTextMock = mockSdkService(
      AiAudioController,
      "audioToText",
      dictatedTextResponse("text")
    )
    mockSdkService(AiController, "suggestTitle", { title: "" })
    wrapper = mountNoteAudioTools(note)
    noteStore.refreshNoteRealm(originalRealm)
    updateContentMock = mockSdkServiceWithImplementation(
      TextContentController,
      "updateNoteContent",
      (options) =>
        makeMe.aNoteRealm
          .id(options.path.note)
          .content(options.body.content ?? "")
          .please()
    )
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("passes isMidSpeech for timer-triggered chunks", async () => {
    await processAudio(
      wrapper,
      midSpeechChunk(new File(["test2"], "test.webm"))
    )

    expect(audioToTextMock).toHaveBeenCalledWith({
      body: expect.objectContaining({
        isMidSpeech: true,
        previousNoteContentToAppendTo: note.content,
      }),
    })
  })

  it("returns endTimestamp from audio processing", async () => {
    audioToTextMock.mockResolvedValue(
      wrapSdkResponse(dictatedTextResponse("text"))
    )
    mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )

    const result = await processAudio(
      wrapper,
      midSpeechChunk(new File(["test"], "test.webm"))
    )

    expect(result).toBe("00:00:37,270")
  })

  it.each([
    { when: "under 500 chars", content: "Short", sent: "Short" },
    {
      when: "over 500 chars",
      content: "a".repeat(600),
      sent: `...${"a".repeat(500)}`,
    },
    { when: "undefined", content: undefined, sent: "" },
  ])(
    "sends previous content, truncated with ellipsis, when $when",
    async ({ content, sent }) => {
      wrapper.unmount()
      const contextRealm = makeMe.aNoteRealm.content(content).please()
      wrapper = mountNoteAudioTools(contextRealm.note)
      noteStore.refreshNoteRealm(contextRealm)

      await processAudio(wrapper, midSpeechChunk())

      expect(audioToTextMock).toHaveBeenCalledWith({
        body: expect.objectContaining({ previousNoteContentToAppendTo: sent }),
      })
    }
  )

  it("loads an absent originating realm for both context and append", async () => {
    noteStore.refOfNoteRealm(note.id).value = undefined
    const loadedBody = "Loaded current body."
    const showNote = mockSdkService(
      NoteController,
      "showNote",
      makeMe.aNoteRealm.id(note.id).content(loadedBody).please()
    )
    await processAudio(wrapper)
    expect(showNote).toHaveBeenCalledExactlyOnceWith({
      path: { note: note.id },
    })
    expect(audioToTextMock).toHaveBeenCalledWith({
      body: expect.objectContaining({
        previousNoteContentToAppendTo: loadedBody,
      }),
    })
    expect(updateContentMock).toHaveBeenCalledWith({
      path: { note: note.id },
      body: { content: `${loadedBody}text` },
    })
  })

  describe("title suggestion", () => {
    let updateNoteTitleSpy: ReturnType<typeof mockSdkService>

    beforeEach(() => {
      updateNoteTitleSpy = mockSdkService(
        TextContentController,
        "updateNoteTitle",
        {} as never
      )
      mockSdkService(
        TextContentController,
        "updateNoteContent",
        makeMe.aNoteRealm.please()
      )
    })

    it("suggests title on power-of-2 audio processes", async () => {
      const suggestTitleSpy = mockSdkService(AiController, "suggestTitle", {
        title: "Suggested Title",
      })

      for (let i = 0; i < 9; i++) {
        await processAudio(wrapper)
      }

      expect(suggestTitleSpy).toHaveBeenCalledTimes(4)
      expect(updateNoteTitleSpy).toHaveBeenCalledTimes(4)
    })

    it("does not update title when suggestion is empty", async () => {
      const suggestTitleSpy = mockSdkService(AiController, "suggestTitle", {
        title: "",
      })

      await processAudio(wrapper)

      expect(suggestTitleSpy).toHaveBeenCalled()
      expect(updateNoteTitleSpy).not.toHaveBeenCalled()
    })
  })
})

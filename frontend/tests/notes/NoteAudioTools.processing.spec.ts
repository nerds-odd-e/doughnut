import {
  AiAudioController,
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
  startRecording,
  stopRecording,
  useNoteAudioToolsTestLifecycle,
  type NoteAudioToolsWrapper,
} from "@tests/notes/noteAudioToolsTestSupport"
import { flushPromises } from "@vue/test-utils"
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
  const originalRealm = makeMe.aNoteRealm
    .title("Author chosen title")
    .content("Original body.")
    .please()
  const note = originalRealm.note
  const noteStore = useNoteStore()
  beforeEach(() => {
    audioToTextMock = mockSdkService(
      AiAudioController,
      "audioToText",
      dictatedTextResponse("text")
    )
    wrapper = mountNoteAudioTools(note)
    noteStore.refreshNoteRealm(originalRealm)
    updateContentMock = mockSdkServiceWithImplementation(
      TextContentController,
      "updateNoteContent",
      (options) =>
        makeMe.aNoteRealm
          .id(options.path.note)
          .title(note.noteTopology.title)
          .content(options.body.content ?? "")
          .please()
    )
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("sends timer-triggered chunks as mid-speech", async () => {
    await processAudio(
      wrapper,
      midSpeechChunk(new File(["test2"], "test.webm"))
    )

    expect(audioToTextMock).toHaveBeenCalledWith({
      body: expect.objectContaining({
        midSpeech: true,
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

  it("never changes the title across many chunks and a later recording", async () => {
    const updateNoteTitle = mockSdkService(
      TextContentController,
      "updateNoteTitle",
      {} as never
    )

    await startRecording(wrapper)
    for (let i = 0; i < 9; i++) {
      await processAudio(wrapper)
    }
    await stopRecording(wrapper)
    await startRecording(wrapper)
    await processAudio(wrapper)
    await flushPromises()

    expect(updateNoteTitle).not.toHaveBeenCalled()
    expect(
      noteStore.refOfNoteRealm(note.id).value?.note.noteTopology.title
    ).toBe("Author chosen title")
  })
})

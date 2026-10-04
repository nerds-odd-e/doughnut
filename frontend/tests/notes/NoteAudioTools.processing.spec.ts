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

  it("sends only the audio and the mid-speech flag", async () => {
    const audio = new File(["test2"], "test.webm")
    await processAudio(wrapper, midSpeechChunk(audio))

    expect(audioToTextMock).toHaveBeenCalledExactlyOnceWith({
      body: { uploadAudioFile: audio, midSpeech: true },
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

  it("loads an absent originating realm to append to it", async () => {
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
    expect(updateContentMock).toHaveBeenCalledWith({
      path: { note: note.id },
      body: { content: `${loadedBody} text` },
    })
  })
})

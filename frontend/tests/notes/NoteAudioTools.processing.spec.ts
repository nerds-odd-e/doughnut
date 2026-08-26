import {
  AiAudioController,
  TextContentController,
} from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import {
  midSpeechChunk,
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
  const note = makeMe.aNote.please()

  beforeEach(() => {
    wrapper = mountNoteAudioTools(note)
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  describe("thread context", () => {
    let audioToTextMock: ReturnType<typeof mockSdkService>

    beforeEach(() => {
      audioToTextMock = mockSdkService(AiAudioController, "audioToText", {
        completionFromAudio: { content: "text" },
        endTimestamp: "00:00:37,270",
      })
    })

    it("reuses previous note content between calls", async () => {
      audioToTextMock
        .mockResolvedValueOnce(
          wrapSdkResponse({
            completionFromAudio: { content: "text1" },
            endTimestamp: "00:00:37,270",
          })
        )
        .mockResolvedValueOnce(
          wrapSdkResponse({
            completionFromAudio: { content: "text2" },
            endTimestamp: "00:00:47,270",
          })
        )

      await processAudio(wrapper)
      expect(audioToTextMock).toHaveBeenLastCalledWith({
        body: expect.objectContaining({
          previousNoteContentToAppendTo: note.content,
        }),
      })

      await processAudio(wrapper)
      expect(audioToTextMock).toHaveBeenLastCalledWith({
        body: expect.objectContaining({
          previousNoteContentToAppendTo: note.content,
        }),
      })
    })

    it("keeps previous content after an API error", async () => {
      const ok = {
        completionFromAudio: { content: "text1" },
        endTimestamp: "00:00:37,270",
      }
      audioToTextMock
        .mockResolvedValueOnce(wrapSdkResponse(ok))
        .mockResolvedValueOnce(wrapSdkError("API Error"))
        .mockResolvedValueOnce(wrapSdkResponse(ok))

      await processAudio(wrapper)
      await processAudio(wrapper)
      await processAudio(wrapper)

      expect(audioToTextMock).toHaveBeenLastCalledWith({
        body: expect.objectContaining({
          previousNoteContentToAppendTo: note.content,
        }),
      })
    })
  })

  it("passes isMidSpeech for timer-triggered chunks", async () => {
    const audioToTextMock = mockSdkService(AiAudioController, "audioToText", {
      completionFromAudio: { content: "text" },
      endTimestamp: "00:00:37,270",
    })

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
    mockSdkService(AiAudioController, "audioToText", {
      completionFromAudio: {
        content: "--- a\n+++ b\n@@ -0,0 +1 @@\n+text\n",
      },
      endTimestamp: "00:00:37,270",
    })
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

  describe("previous content truncation", () => {
    let audioToTextMock: ReturnType<typeof mockSdkService>

    beforeEach(() => {
      audioToTextMock = mockSdkService(AiAudioController, "audioToText", {
        completionFromAudio: { content: "text" },
        endTimestamp: "00:00:37,270",
      })
    })

    it("sends full content when under 500 characters", async () => {
      const shortContent = "Short content"
      wrapper.unmount()
      wrapper = mountNoteAudioTools(makeMe.aNote.content(shortContent).please())

      await processAudio(wrapper, midSpeechChunk())

      expect(audioToTextMock).toHaveBeenCalledWith({
        body: expect.objectContaining({
          previousNoteContentToAppendTo: shortContent,
        }),
      })
    })

    it("truncates content over 500 characters with ellipsis", async () => {
      const longContent = "a".repeat(600)
      wrapper.unmount()
      wrapper = mountNoteAudioTools(makeMe.aNote.content(longContent).please())

      await processAudio(wrapper, midSpeechChunk())

      expect(audioToTextMock).toHaveBeenCalledWith({
        body: expect.objectContaining({
          previousNoteContentToAppendTo: `...${"a".repeat(500)}`,
        }),
      })
    })

    it("sends empty string when note content is undefined", async () => {
      wrapper.unmount()
      wrapper = mountNoteAudioTools(makeMe.aNote.content(undefined).please())

      await processAudio(wrapper, midSpeechChunk())

      expect(audioToTextMock).toHaveBeenCalledWith({
        body: expect.objectContaining({
          previousNoteContentToAppendTo: "",
        }),
      })
    })
  })
})

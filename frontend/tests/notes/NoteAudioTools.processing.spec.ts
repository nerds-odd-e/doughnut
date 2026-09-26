import {
  AiAudioController,
  AiController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
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
  let audioToTextMock: ReturnType<typeof mockSdkService>
  const note = makeMe.aNote.please()
  const textResponse = (content: string, endTimestamp = "00:00:37,270") => ({
    completionFromAudio: { content },
    endTimestamp,
  })

  beforeEach(() => {
    audioToTextMock = mockSdkService(
      AiAudioController,
      "audioToText",
      textResponse("text")
    )
    wrapper = mountNoteAudioTools(note)
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("reuses previous note content between calls", async () => {
    audioToTextMock
      .mockResolvedValueOnce(wrapSdkResponse(textResponse("text1")))
      .mockResolvedValueOnce(
        wrapSdkResponse(textResponse("text2", "00:00:47,270"))
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
    audioToTextMock
      .mockResolvedValueOnce(wrapSdkResponse(textResponse("text1")))
      .mockResolvedValueOnce(wrapSdkError("API Error"))
      .mockResolvedValueOnce(wrapSdkResponse(textResponse("text1")))

    await processAudio(wrapper)
    await processAudio(wrapper)
    await processAudio(wrapper)

    expect(audioToTextMock).toHaveBeenLastCalledWith({
      body: expect.objectContaining({
        previousNoteContentToAppendTo: note.content,
      }),
    })
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
      wrapSdkResponse(textResponse("--- a\n+++ b\n@@ -0,0 +1 @@\n+text\n"))
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
      wrapper = mountNoteAudioTools(makeMe.aNote.content(content).please())

      await processAudio(wrapper, midSpeechChunk())

      expect(audioToTextMock).toHaveBeenCalledWith({
        body: expect.objectContaining({ previousNoteContentToAppendTo: sent }),
      })
    }
  )

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

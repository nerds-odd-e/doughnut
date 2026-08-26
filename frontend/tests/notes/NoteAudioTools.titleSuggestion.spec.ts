import {
  AiAudioController,
  AiController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import {
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

describe("NoteAudioTools title suggestion", () => {
  let wrapper: NoteAudioToolsWrapper
  let updateNoteTitleSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    updateNoteTitleSpy = mockSdkService(
      TextContentController,
      "updateNoteTitle",
      {} as never
    )
    mockSdkService(AiAudioController, "audioToText", {
      completionFromAudio: { content: "text" },
      endTimestamp: "00:00:00,000",
    })
    wrapper = mountNoteAudioTools(makeMe.aNote.title("Untitled").please())
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("suggests title on power-of-2 audio processes", async () => {
    const suggestTitleSpy = mockSdkService(AiController, "suggestTitle", {
      title: "Suggested Title",
    })
    mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )

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
    mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )

    await processAudio(wrapper)

    expect(suggestTitleSpy).toHaveBeenCalled()
    expect(updateNoteTitleSpy).not.toHaveBeenCalled()
  })
})

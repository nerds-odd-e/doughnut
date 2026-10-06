import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { useNoteStore } from "@/store/noteStore"
import {
  audioTextResponse,
  audioToolsVm,
  dictationStatus,
  findButtonByText,
  midSpeechChunk,
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

describe("NoteAudioTools dictation status", () => {
  let wrapper: NoteAudioToolsWrapper
  const realm = makeMe.aNoteRealm.please()

  beforeEach(() => {
    wrapper = mountNoteAudioTools(realm.note)
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("says it is turning speech into text until Stop has finished", async () => {
    let finishStop!: (file: File) => void
    await startRecording(wrapper)
    audioToolsVm(wrapper).audioRecorder.stopRecording.mockImplementation(
      () => new Promise<File>((resolve) => (finishStop = resolve))
    )

    await stopRecording(wrapper)
    expect(dictationStatus(wrapper)).toBe("Turning your speech into text…")

    finishStop(new File([], "test.webm"))
    await flushPromises()
    expect(dictationStatus(wrapper)).toBe("Ready to record")
    expect(findButtonByText(wrapper, "Record")).toBeTruthy()
  })

  it("keeps recording status and Stop when a mid-speech conversion finishes", async () => {
    useNoteStore().refreshNoteRealm(realm)
    mockSdkService(AiAudioController, "audioToText", audioTextResponse("test"))
    const saveContent = mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )
    await startRecording(wrapper)

    await processAudio(wrapper, midSpeechChunk())
    await flushPromises()

    expect(saveContent).toHaveBeenCalled()

    expect(dictationStatus(wrapper)).toBe("Recording. Speak now.")
    expect(findButtonByText(wrapper, "Stop")).toBeTruthy()
    expect(findButtonByText(wrapper, "Record")).toBeUndefined()
  })
})

import {
  AiAudioController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { useNoteStore } from "@/store/noteStore"
import {
  audioChunk,
  audioTextResponse,
  expectIdleVoiceInputButton,
  mountNoteVoiceInputButton,
  processAudio,
  startRecording,
  stopRecording,
  useNoteVoiceInputTestLifecycle,
  voiceInputVm,
  type NoteVoiceInputButtonWrapper,
} from "@tests/notes/noteVoiceInputButtonTestSupport"
import {
  showToastsOnPage,
  toastMessage,
  toastShown,
} from "@tests/helpers/toastTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

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

describe("NoteVoiceInputButton after a failed conversion at Stop", () => {
  let wrapper: NoteVoiceInputButtonWrapper
  let audioToText: ReturnType<typeof mockSdkService>
  let saveContent: ReturnType<typeof mockSdkService>
  const realm = makeMe.aNoteRealm.content("Original body.").please()

  beforeEach(() => {
    saveContent = mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )
    audioToText = mockSdkService(
      AiAudioController,
      "audioToText",
      audioTextResponse("hello")
    ).mockResolvedValue(wrapSdkError("API Error"))
    wrapper = mountNoteVoiceInputButton(realm.note)
    useNoteStore().refreshNoteRealm(realm)
    const recorder = voiceInputVm(wrapper).audioRecorder
    recorder.hasUnconvertedAudio.mockReturnValue(true)
    recorder.stopRecording.mockImplementation(async () => {
      await processAudio(wrapper, audioChunk()).catch(() => undefined)
    })
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  const recordAndStop = async () => {
    await startRecording(wrapper)
    await stopRecording(wrapper)
  }

  it("toasts that the recording is kept and returns to the idle button", async () => {
    await recordAndStop()

    expect(toastMessage(await toastShown("error"))).toBe(
      "Could not turn your speech into text. Your recording is kept."
    )
    expectIdleVoiceInputButton(wrapper)
    expect(saveContent).not.toHaveBeenCalled()
  })

  it("turns the kept recording into text once with the next recording", async () => {
    await recordAndStop()
    audioToText.mockResolvedValue(wrapSdkResponse(audioTextResponse("hello")))

    await recordAndStop()

    expectIdleVoiceInputButton(wrapper)
    expect(saveContent).toHaveBeenCalledExactlyOnceWith({
      path: { note: realm.note.id },
      body: { content: "Original body. hello" },
    })
  })
})

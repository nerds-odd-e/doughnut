import { AiAudioController } from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { wrapSdkError } from "@tests/helpers"
import {
  audioChunk,
  audioToolsVm,
  findButtonByTitle,
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

describe("NoteAudioTools Retry after a failed conversion", () => {
  let wrapper: NoteAudioToolsWrapper
  const note = makeMe.aNote.please()

  beforeEach(() => {
    wrapper = mountNoteAudioTools(note)
    audioToolsVm(wrapper).audioRecorder.hasUnconvertedAudio.mockReturnValue(
      true
    )
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  const failConversion = async () => {
    vi.spyOn(AiAudioController, "audioToText").mockResolvedValue(
      wrapSdkError("API Error")
    )
    await expect(processAudio(wrapper, audioChunk())).rejects.toThrow()
    await flushPromises()
  }
  const retryButton = () => findButtonByTitle(wrapper, "Retry")

  it("offers Retry after Stop while audio is not converted", async () => {
    await startRecording(wrapper)
    await failConversion()
    expect(retryButton()).toBeUndefined()

    await stopRecording(wrapper)

    expect(retryButton()).toBeTruthy()
  })

  it("does not offer Retry when nothing remains to convert", async () => {
    audioToolsVm(wrapper).audioRecorder.hasUnconvertedAudio.mockReturnValue(
      false
    )
    await startRecording(wrapper)
    await failConversion()
    await stopRecording(wrapper)

    expect(retryButton()).toBeUndefined()
  })

  it("converts what is left again and keeps Retry when it fails again", async () => {
    await startRecording(wrapper)
    await failConversion()
    await stopRecording(wrapper)
    const recorder = audioToolsVm(wrapper).audioRecorder
    recorder.stopRecording.mockImplementationOnce(async () => {
      await processAudio(wrapper, audioChunk()).catch(() => undefined)
      return new File([], "test.webm")
    })

    await retryButton()!.trigger("click")
    await flushPromises()

    expect(recorder.stopRecording).toHaveBeenCalledTimes(2)
    expect(retryButton()).toBeTruthy()
    expect(wrapper.find(".daisy-alert-error").exists()).toBe(true)
  })
})

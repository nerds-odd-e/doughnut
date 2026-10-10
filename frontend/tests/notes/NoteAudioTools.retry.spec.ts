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
  audioToolsVm,
  findButtonByText,
  mountNoteAudioTools,
  processAudio,
  startRecording,
  stopRecording,
  useNoteAudioToolsTestLifecycle,
  type NoteAudioToolsWrapper,
} from "@tests/notes/noteAudioToolsTestSupport"
import {
  showToastsOnPage,
  toastMessage,
  toastMessagesOnPage,
  toastShown,
} from "@tests/helpers/toastTestSupport"
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
showToastsOnPage()

describe("NoteAudioTools Retry after a failed conversion", () => {
  const failedAtStop =
    "Could not turn your speech into text. Your recording is kept until you close Audio tools."
  let wrapper: NoteAudioToolsWrapper
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
    wrapper = mountNoteAudioTools(realm.note)
    useNoteStore().refreshNoteRealm(realm)
    const recorder = audioToolsVm(wrapper).audioRecorder
    recorder.hasUnconvertedAudio.mockReturnValue(true)
    recorder.stopRecording.mockImplementation(async () => {
      await processAudio(wrapper, audioChunk()).catch(() => undefined)
    })
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  const retryButton = () => findButtonByText(wrapper, "Retry")
  const failAtStop = async () => {
    await startRecording(wrapper)
    await stopRecording(wrapper)
  }

  it("names every control in words when ready, recording, and failed after Stop", async () => {
    const unnamedControls = () =>
      wrapper
        .findAll("button, select")
        .filter(
          (control) =>
            !control.text().trim() && !control.attributes("aria-label")
        )

    expect(unnamedControls()).toEqual([])
    await startRecording(wrapper)
    expect(wrapper.find("select").attributes("aria-label")).toBe("Microphone")
    expect(unnamedControls()).toEqual([])
    await stopRecording(wrapper)
    expect(retryButton()).toBeTruthy()
    expect(unnamedControls()).toEqual([])
  })

  it("toasts at Stop that the recording is kept, and offers Retry and Record", async () => {
    await failAtStop()

    expect(toastMessage(await toastShown("error"))).toBe(failedAtStop)
    expect(wrapper.text()).toBe("Retry Record")
    expect(
      findButtonByText(wrapper, "Record")!.attributes()
    ).not.toHaveProperty("disabled")
    expect(saveContent).not.toHaveBeenCalled()
  })

  it("does not offer Retry when nothing remains to convert", async () => {
    audioToolsVm(wrapper).audioRecorder.hasUnconvertedAudio.mockReturnValue(
      false
    )
    await failAtStop()

    expect(retryButton()).toBeUndefined()
  })

  it("turns the kept recording into text once", async () => {
    await failAtStop()
    audioToText.mockResolvedValue(wrapSdkResponse(audioTextResponse("hello")))

    await retryButton()!.trigger("click")
    await flushPromises()

    expect(retryButton()).toBeUndefined()
    expect(saveContent).toHaveBeenCalledExactlyOnceWith({
      path: { note: realm.note.id },
      body: { content: "Original body. hello" },
    })
  })

  it("does not let Record start while Retry finishes", async () => {
    await failAtStop()
    audioToText.mockResolvedValue(wrapSdkResponse(audioTextResponse("hello")))
    const recorder = audioToolsVm(wrapper).audioRecorder
    let finishRetry!: () => void
    recorder.stopRecording.mockImplementation(async () => {
      await new Promise<void>((resolve) => (finishRetry = resolve))
      await processAudio(wrapper, audioChunk())
    })

    await retryButton()!.trigger("click")
    await flushPromises()
    expect(findButtonByText(wrapper, "Record")!.attributes()).toHaveProperty(
      "disabled"
    )
    await startRecording(wrapper)
    expect(recorder.startRecording).toHaveBeenCalledTimes(1)

    finishRetry()
    await flushPromises()
    expect(
      findButtonByText(wrapper, "Record")!.attributes()
    ).not.toHaveProperty("disabled")
  })

  it("toasts again and keeps Retry when Retry fails again", async () => {
    await failAtStop()
    await toastShown("error")

    await retryButton()!.trigger("click")
    await flushPromises()

    expect(
      audioToolsVm(wrapper).audioRecorder.stopRecording
    ).toHaveBeenCalledTimes(2)
    await vi.waitFor(() =>
      expect(toastMessagesOnPage()).toEqual([failedAtStop, failedAtStop])
    )
    expect(retryButton()).toBeTruthy()
    expect(saveContent).not.toHaveBeenCalled()
  })
})

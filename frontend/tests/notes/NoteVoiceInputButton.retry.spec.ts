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
  voiceInputButton,
  voiceInputVm,
  type NoteVoiceInputButtonWrapper,
} from "@tests/notes/noteVoiceInputButtonTestSupport"
import {
  showToastsOnPage,
  toastMessage,
  toastMessagesOnPage,
  toastShown,
} from "@tests/helpers/toastTestSupport"
import { noteVoiceInputTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { mockMediaDevices } from "@tests/notes/noteVoiceInputButtonMocks"
import { flushPromises } from "@vue/test-utils"
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
  const failedAtStop =
    "Could not turn your speech into text. Your recording is kept until you leave this note; click Voice input to try again."
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

  const failAtStop = async () => {
    await startRecording(wrapper)
    await stopRecording(wrapper)
  }
  const retry = async () => {
    await wrapper
      .find(`button[aria-label="${noteVoiceInputTitles.retry}"]`)
      .trigger("click")
    await flushPromises()
  }
  const expectKeptRecordingButton = () => {
    const button = voiceInputButton(wrapper)
    expect(button.attributes("aria-label")).toBe(
      "Retry turning your speech into text"
    )
    expect(button.attributes("title")).toBe(
      "Retry turning your speech into text"
    )
    expect(button.attributes()).not.toHaveProperty("disabled")
    expect(button.attributes()).not.toHaveProperty("aria-pressed")
    expect(button.classes()).toContain("daisy-btn-warning")
    expect(button.classes()).not.toContain("daisy-btn-ghost")
    expect(button.classes()).not.toContain("daisy-btn-primary")
  }

  it("toasts at Stop how to retry, and the button holds the kept recording", async () => {
    await failAtStop()

    expect(toastMessage(await toastShown("error"))).toBe(failedAtStop)
    expectKeptRecordingButton()
    expect(saveContent).not.toHaveBeenCalled()
  })

  it("leaves the idle button when nothing remains to convert", async () => {
    voiceInputVm(wrapper).audioRecorder.hasUnconvertedAudio.mockReturnValue(
      false
    )
    await failAtStop()

    expectIdleVoiceInputButton(wrapper)
  })

  it("turns the kept recording into text once without the microphone", async () => {
    await failAtStop()
    audioToText.mockResolvedValue(wrapSdkResponse(audioTextResponse("hello")))

    await retry()

    expectIdleVoiceInputButton(wrapper)
    expect(mockMediaDevices.getUserMedia).toHaveBeenCalledTimes(1)
    expect(audioToText).toHaveBeenCalledTimes(2)
    expect(saveContent).toHaveBeenCalledExactlyOnceWith({
      path: { note: realm.note.id },
      body: { content: "Original body. hello" },
    })
  })

  it("is unavailable while the retry runs", async () => {
    await failAtStop()
    audioToText.mockResolvedValue(wrapSdkResponse(audioTextResponse("hello")))
    let finishRetry!: () => void
    voiceInputVm(wrapper).audioRecorder.stopRecording.mockImplementation(
      async () => {
        await new Promise<void>((resolve) => (finishRetry = resolve))
        await processAudio(wrapper, audioChunk())
      }
    )

    await retry()

    const button = voiceInputButton(wrapper)
    expect(button.attributes()).toHaveProperty("disabled")
    expect(button.attributes("aria-label")).toBe("Voice input")

    finishRetry()
    await flushPromises()
    expectIdleVoiceInputButton(wrapper)
  })

  it("toasts again and keeps the recording when the retry fails", async () => {
    await failAtStop()
    await toastShown("error")

    await retry()

    await vi.waitFor(() =>
      expect(toastMessagesOnPage()).toEqual([failedAtStop, failedAtStop])
    )
    expectKeptRecordingButton()
    expect(saveContent).not.toHaveBeenCalled()
  })
})

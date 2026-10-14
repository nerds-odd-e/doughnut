import {
  expectIdleVoiceInputButton,
  mountNoteVoiceInputButton,
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
  toastShown,
} from "@tests/helpers/toastTestSupport"
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

describe("NoteVoiceInputButton recording", () => {
  let wrapper: NoteVoiceInputButtonWrapper

  beforeEach(() => {
    wrapper = mountNoteVoiceInputButton()
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  const recorder = () => voiceInputVm(wrapper).audioRecorder

  it("is an idle button, then a pressed one drawing the microphone's level, then idle again", async () => {
    expectIdleVoiceInputButton(wrapper)
    expect(voiceInputButton(wrapper).attributes()).not.toHaveProperty(
      "aria-pressed"
    )

    await startRecording(wrapper)

    const button = voiceInputButton(wrapper)
    expect(button.attributes("aria-label")).toBe("Stop voice input")
    expect(button.attributes("title")).toBe("Stop voice input")
    expect(button.attributes("aria-pressed")).toBe("true")
    expect(button.classes()).toEqual(
      expect.arrayContaining(["daisy-btn-soft", "daisy-btn-primary"])
    )
    expect(button.find("canvas").exists()).toBe(true)
    expect(recorder().getAudioData).toHaveBeenCalled()

    await stopRecording(wrapper)

    expectIdleVoiceInputButton(wrapper)
    expect(voiceInputButton(wrapper).find("canvas").exists()).toBe(false)
  })

  it("starts recording with wake lock and Web Audio connections", async () => {
    const {
      mockMediaDevices,
      mockMediaStreamSource,
      mockAudioWorkletNode,
      mockAudioContext,
    } = await import("@tests/notes/noteVoiceInputButtonMocks")

    await startRecording(wrapper)

    expect(recorder().startRecording).toHaveBeenCalled()
    expect(voiceInputVm(wrapper).wakeLocker.request).toHaveBeenCalled()
    expect(mockMediaDevices.getUserMedia).toHaveBeenCalledWith({ audio: true })
    expect(mockMediaStreamSource.connect).toHaveBeenCalledWith(
      mockAudioWorkletNode
    )
    expect(mockAudioWorkletNode.connect).toHaveBeenCalledWith(
      mockAudioContext.destination
    )
  })

  it("stops recording, cleans up audio graph, and releases wake lock", async () => {
    const { mockAudioWorkletNode, mockMediaStreamSource, mockMediaStop } =
      await import("@tests/notes/noteVoiceInputButtonMocks")

    await startRecording(wrapper)
    await stopRecording(wrapper)

    expect(recorder().stopRecording).toHaveBeenCalled()
    expect(mockAudioWorkletNode.disconnect).toHaveBeenCalled()
    expect(mockMediaStreamSource.disconnect).toHaveBeenCalled()
    expect(mockMediaStop).toHaveBeenCalled()
    expect(voiceInputVm(wrapper).wakeLocker.release).toHaveBeenCalled()
  })

  it("can start a second recording after stop", async () => {
    await startRecording(wrapper)
    await stopRecording(wrapper)
    await startRecording(wrapper)

    expect(recorder().startRecording).toHaveBeenCalledTimes(2)
  })

  it("explains a microphone that cannot be used and keeps the idle button", async () => {
    recorder().startRecording.mockRejectedValueOnce(
      new Error("Permission denied")
    )
    await startRecording(wrapper)

    expect(toastMessage(await toastShown("error"))).toBe(
      "Could not use the microphone. Allow microphone access in your browser, then try again."
    )
    expectIdleVoiceInputButton(wrapper)

    await startRecording(wrapper)
    expect(voiceInputButton(wrapper).attributes("aria-pressed")).toBe("true")
  })

  it("stops recording when unmounted while recording", async () => {
    await startRecording(wrapper)
    const stopRecorder = recorder().stopRecording

    wrapper.unmount()
    await flushPromises()

    expect(stopRecorder).toHaveBeenCalled()
  })
})

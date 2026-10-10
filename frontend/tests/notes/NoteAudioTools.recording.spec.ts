import {
  audioToolsVm,
  findButtonByText,
  mountNoteAudioTools,
  startRecording,
  stopRecording,
  useNoteAudioToolsTestLifecycle,
  type NoteAudioToolsWrapper,
} from "@tests/notes/noteAudioToolsTestSupport"
import {
  showToastsOnPage,
  toastMessage,
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

describe("NoteAudioTools recording controls", () => {
  let wrapper: NoteAudioToolsWrapper

  beforeEach(() => {
    wrapper = mountNoteAudioTools()
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  const buttonTexts = () =>
    wrapper.findAll("button").map((button) => button.text())

  it("offers only Record when ready, then Stop, Write text now and the microphone chooser while recording", async () => {
    expect(wrapper.text()).toBe("Record")

    await startRecording(wrapper)

    expect(buttonTexts()).toEqual(["Stop", "Write text now"])
    expect(wrapper.find(".device-select").exists()).toBe(true)
    expect(
      findButtonByText(wrapper, "Write text now")!.attributes("disabled")
    ).toBeUndefined()
  })

  it("starts recording with wake lock and Web Audio connections", async () => {
    const {
      mockMediaDevices,
      mockMediaStreamSource,
      mockAudioWorkletNode,
      mockAudioContext,
    } = await import("@tests/notes/noteAudioToolsMocks")

    await startRecording(wrapper)

    expect(
      audioToolsVm(wrapper).audioRecorder.startRecording
    ).toHaveBeenCalled()
    expect(audioToolsVm(wrapper).wakeLocker.request).toHaveBeenCalled()
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
      await import("@tests/notes/noteAudioToolsMocks")

    await startRecording(wrapper)
    await stopRecording(wrapper)

    expect(audioToolsVm(wrapper).audioRecorder.stopRecording).toHaveBeenCalled()
    expect(audioToolsVm(wrapper).isRecording).toBe(false)
    expect(mockAudioWorkletNode.disconnect).toHaveBeenCalled()
    expect(mockMediaStreamSource.disconnect).toHaveBeenCalled()
    expect(mockMediaStop).toHaveBeenCalled()
    expect(audioToolsVm(wrapper).wakeLocker.release).toHaveBeenCalled()
  })

  it("loads devices and switches selection while recording", async () => {
    const { mockDevices, mockMediaDevices } = await import(
      "@tests/notes/noteAudioToolsMocks"
    )

    await startRecording(wrapper)

    const deviceSelect = wrapper.find(".device-select")
    expect(deviceSelect.exists()).toBe(true)
    expect(deviceSelect.findAll("option")).toHaveLength(mockDevices.length)
    expect(mockMediaDevices.enumerateDevices).toHaveBeenCalled()

    await deviceSelect.setValue("device2")
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(
      audioToolsVm(wrapper).audioRecorder.switchAudioDevice
    ).toHaveBeenCalledWith("device2")
    expect(mockMediaDevices.getUserMedia).toHaveBeenCalledWith({
      audio: { deviceId: { exact: "device2" } },
    })
  })

  it("toasts a microphone switch that fails and keeps recording", async () => {
    await startRecording(wrapper)
    audioToolsVm(wrapper).audioRecorder.switchAudioDevice.mockRejectedValueOnce(
      new Error("Device gone")
    )

    await wrapper.find(".device-select").setValue("device2")
    await flushPromises()

    expect(toastMessage(await toastShown("error"))).toBe(
      "Failed to switch audio device"
    )
    expect(buttonTexts()).toEqual(["Stop", "Write text now"])
  })

  it("can start a second recording after stop", async () => {
    await startRecording(wrapper)
    await stopRecording(wrapper)
    await startRecording(wrapper)

    expect(
      audioToolsVm(wrapper).audioRecorder.startRecording
    ).toHaveBeenCalledTimes(2)
  })

  it("converts what has been said so far with Write text now", async () => {
    await startRecording(wrapper)
    await findButtonByText(wrapper, "Write text now")!.trigger("click")

    expect(audioToolsVm(wrapper).audioRecorder.tryFlush).toHaveBeenCalled()
  })

  it("explains a microphone that cannot be used and keeps Record available", async () => {
    audioToolsVm(wrapper).audioRecorder.startRecording.mockRejectedValueOnce(
      new Error("Permission denied")
    )
    await startRecording(wrapper)

    expect(toastMessage(await toastShown("error"))).toBe(
      "Could not use the microphone. Allow microphone access in your browser, then try again."
    )
    expect(wrapper.text()).toBe("Record")

    await startRecording(wrapper)
    expect(buttonTexts()).toEqual(["Stop", "Write text now"])
  })

  it("stops recording when unmounted while recording", async () => {
    await startRecording(wrapper)
    const vm = audioToolsVm(wrapper)

    wrapper.unmount()
    await flushPromises()

    expect(vm.isRecording).toBe(false)
    expect(vm.audioRecorder.stopRecording).toHaveBeenCalled()
  })
})

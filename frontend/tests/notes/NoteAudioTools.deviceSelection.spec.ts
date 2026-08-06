import makeMe from "doughnut-test-fixtures/makeMe"
import {
  audioToolsVm,
  mountNoteAudioTools,
  startRecording,
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

describe("NoteAudioTools device selection", () => {
  let wrapper: NoteAudioToolsWrapper

  beforeEach(() => {
    wrapper = mountNoteAudioTools(makeMe.aNote.please())
  })

  afterEach(() => {
    wrapper?.unmount()
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
})

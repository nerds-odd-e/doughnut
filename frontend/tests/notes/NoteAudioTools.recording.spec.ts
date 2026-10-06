import { AiAudioController } from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkServiceWithImplementation } from "@tests/helpers"
import {
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

describe("NoteAudioTools recording controls", () => {
  let wrapper: NoteAudioToolsWrapper
  const note = makeMe.aNote.please()

  beforeEach(() => {
    wrapper = mountNoteAudioTools(note)
  })

  afterEach(() => {
    wrapper?.unmount()
  })

  it("offers Record when ready, then Stop while recording, with the status announced", async () => {
    expect(dictationStatus(wrapper)).toBe("Ready to record")
    expect(findButtonByText(wrapper, "Record")).toBeTruthy()
    expect(findButtonByText(wrapper, "Stop")).toBeUndefined()
    expect(findButtonByText(wrapper, "Write text now")).toBeUndefined()

    await startRecording(wrapper)

    expect(dictationStatus(wrapper)).toBe("Recording. Speak now.")
    expect(findButtonByText(wrapper, "Record")).toBeUndefined()
    expect(findButtonByText(wrapper, "Stop")).toBeTruthy()
    expect(wrapper.find(".device-select").exists()).toBe(true)
    expect(
      findButtonByText(wrapper, "Write text now")!.attributes("disabled")
    ).toBeUndefined()
    expect(
      findButtonByText(wrapper, "Save audio")!.attributes("disabled")
    ).toBeDefined()
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

  it("disables Write text now during a conversion", async () => {
    await startRecording(wrapper)
    const writeNowButton = findButtonByText(wrapper, "Write text now")!

    type AudioResponse = {
      segmentTexts: string[]
      endTimestamp: string
    }
    let resolveProcess!: (value: AudioResponse) => void
    const processPromise = new Promise<AudioResponse>((resolve) => {
      resolveProcess = resolve
    })
    mockSdkServiceWithImplementation(
      AiAudioController,
      "audioToText",
      async () => await processPromise
    )

    const processing = processAudio(wrapper, midSpeechChunk())
    await flushPromises()
    expect(writeNowButton.attributes("disabled")).toBeDefined()

    resolveProcess({
      segmentTexts: ["test"],
      endTimestamp: "00:00:37,270",
    })
    await processing
    await flushPromises()
    expect(writeNowButton.attributes("disabled")).toBeFalsy()
  })

  it("stops recording when unmounted while recording", async () => {
    await startRecording(wrapper)
    const vm = audioToolsVm(wrapper)

    wrapper.unmount()
    await flushPromises()

    expect(vm.isRecording).toBe(false)
    expect(vm.audioRecorder.stopRecording).toHaveBeenCalled()
  })

  it("enables Save audio after a recording produces a file", async () => {
    const saveButton = findButtonByText(wrapper, "Save audio")!
    expect(saveButton.attributes("disabled")).toBeDefined()

    await startRecording(wrapper)
    await stopRecording(wrapper)
    audioToolsVm(wrapper).audioFile = new File([], "test.webm")
    await wrapper.vm.$nextTick()

    expect(saveButton.attributes("disabled")).toBeFalsy()
  })

  it("downloads audio via object URL when Save audio is clicked", async () => {
    const { mockCreateObjectURL } = await import(
      "@tests/notes/noteAudioToolsMocks"
    )
    const audioFile = new File([], "test.webm")
    audioToolsVm(wrapper).audioFile = audioFile
    await wrapper.vm.$nextTick()

    const mockAppendChild = vi.spyOn(document.body, "appendChild")
    const mockRemoveChild = vi.spyOn(document.body, "removeChild")
    const mockClick = vi.spyOn(HTMLAnchorElement.prototype, "click")

    await findButtonByText(wrapper, "Save audio")!.trigger("click")

    expect(URL.createObjectURL).toHaveBeenCalledWith(audioFile)
    expect(mockAppendChild).toHaveBeenCalled()
    expect(mockClick).toHaveBeenCalled()
    expect(mockRemoveChild).toHaveBeenCalled()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith(
      mockCreateObjectURL(audioFile)
    )

    mockAppendChild.mockRestore()
    mockRemoveChild.mockRestore()
    mockClick.mockRestore()
  })
})

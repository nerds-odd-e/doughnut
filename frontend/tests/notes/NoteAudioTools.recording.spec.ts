import { AiAudioController } from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockSdkServiceWithImplementation } from "@tests/helpers"
import {
  audioToolsVm,
  findButtonByTitle,
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

  it("shows record and stop controls initially", () => {
    expect(findButtonByTitle(wrapper, "Record Audio")).toBeTruthy()
    expect(findButtonByTitle(wrapper, "Stop Recording")).toBeTruthy()
    expect(findButtonByTitle(wrapper, "Flush Audio")).toBeTruthy()
    expect(findButtonByTitle(wrapper, "Save Audio Locally")).toBeTruthy()
  })

  it("hides Record and enables Stop and Flush while recording", async () => {
    expect(
      findButtonByTitle(wrapper, "Stop Recording")!.attributes("disabled")
    ).toBeDefined()
    expect(
      findButtonByTitle(wrapper, "Flush Audio")!.attributes("disabled")
    ).toBeDefined()

    await startRecording(wrapper)

    expect(findButtonByTitle(wrapper, "Record Audio")).toBeUndefined()
    expect(
      findButtonByTitle(wrapper, "Stop Recording")!.attributes("disabled")
    ).toBeFalsy()
    expect(
      findButtonByTitle(wrapper, "Flush Audio")!.attributes("disabled")
    ).toBeFalsy()
    expect(
      findButtonByTitle(wrapper, "Save Audio Locally")!.attributes("disabled")
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

  it("can start a second recording after stop", async () => {
    await startRecording(wrapper)
    await stopRecording(wrapper)
    await startRecording(wrapper)

    expect(
      audioToolsVm(wrapper).audioRecorder.startRecording
    ).toHaveBeenCalledTimes(2)
  })

  it("flushes audio while recording", async () => {
    await startRecording(wrapper)
    await findButtonByTitle(wrapper, "Flush Audio")!.trigger("click")

    expect(audioToolsVm(wrapper).audioRecorder.tryFlush).toHaveBeenCalled()
  })

  it("disables Flush while audio is processing", async () => {
    await startRecording(wrapper)
    const flushButton = findButtonByTitle(wrapper, "Flush Audio")!

    type AudioResponse = {
      completionFromAudio: { content: string }
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
    expect(flushButton.attributes("disabled")).toBeDefined()

    resolveProcess({
      completionFromAudio: { content: "test" },
      endTimestamp: "00:00:37,270",
    })
    await processing
    await flushPromises()
    expect(flushButton.attributes("disabled")).toBeFalsy()
  })

  it("emits closeDialog when close is clicked", async () => {
    await wrapper.find(".close-btn").trigger("click")
    expect(wrapper.emitted().closeDialog).toBeTruthy()
  })

  it("stops recording when close is clicked while recording", async () => {
    await startRecording(wrapper)
    await wrapper.find(".close-btn").trigger("click")
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(audioToolsVm(wrapper).isRecording).toBe(false)
    expect(audioToolsVm(wrapper).audioRecorder.stopRecording).toHaveBeenCalled()
    expect(wrapper.emitted().closeDialog).toBeTruthy()
  })

  it("enables Save Audio Locally after a recording produces a file", async () => {
    const saveButton = findButtonByTitle(wrapper, "Save Audio Locally")!
    expect(saveButton.attributes("disabled")).toBeDefined()

    await startRecording(wrapper)
    await stopRecording(wrapper)
    audioToolsVm(wrapper).audioFile = new File([], "test.webm")
    await wrapper.vm.$nextTick()

    expect(saveButton.attributes("disabled")).toBeFalsy()
  })

  it("downloads audio via object URL when Save Audio Locally is clicked", async () => {
    const { mockCreateObjectURL } = await import(
      "@tests/notes/noteAudioToolsMocks"
    )
    const audioFile = new File([], "test.webm")
    audioToolsVm(wrapper).audioFile = audioFile
    await wrapper.vm.$nextTick()

    const mockAppendChild = vi.spyOn(document.body, "appendChild")
    const mockRemoveChild = vi.spyOn(document.body, "removeChild")
    const mockClick = vi.spyOn(HTMLAnchorElement.prototype, "click")

    await findButtonByTitle(wrapper, "Save Audio Locally")!.trigger("click")

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

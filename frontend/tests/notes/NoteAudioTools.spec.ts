import { flushPromises } from "@vue/test-utils"
import NoteAudioTools from "@/components/notes/accessory/NoteAudioTools.vue"
import helper from "../helpers"
import { vi } from "vitest"

const mockMediaStreamSource = {
  connect: vi.fn(),
  disconnect: vi.fn(),
}

const mockScriptProcessor = {
  connect: vi.fn(),
  disconnect: vi.fn(),
  onaudioprocess: null,
}

const mockAudioContext = {
  createMediaStreamSource: () => mockMediaStreamSource,
  createScriptProcessor: () => mockScriptProcessor,
  destination: {},
}

// Mock navigator.mediaDevices
const mockMediaDevices = {
  getUserMedia: vi.fn().mockResolvedValue({
    getTracks: () => [
      {
        stop: vi.fn(),
      },
    ],
  }),
}

// Apply mocks to global object
Object.defineProperty(global, "AudioContext", {
  writable: true,
  value: vi.fn(() => mockAudioContext),
})

Object.defineProperty(global.navigator, "mediaDevices", {
  value: mockMediaDevices,
  writable: true,
})

describe("NoteAudioTools", () => {
  let wrapper
  const noteId = 1

  beforeEach(() => {
    wrapper = helper
      .component(NoteAudioTools)
      .withStorageProps({
        noteId,
      })
      .mount()

    // Reset Web Audio API mocks
    mockMediaStreamSource.connect.mockClear()
    mockMediaStreamSource.disconnect.mockClear()
    mockScriptProcessor.connect.mockClear()
    mockScriptProcessor.disconnect.mockClear()
    mockMediaDevices.getUserMedia.mockClear()
  })

  const findButtonByText = (wrapper, text: string) => {
    return wrapper
      .findAll("button")
      .find((button) => button.text().trim() === text)
  }

  it("renders the component with correct buttons", () => {
    expect(findButtonByText(wrapper, "Record Audio")).toBeTruthy()
    expect(findButtonByText(wrapper, "Stop Recording")).toBeTruthy()
    expect(wrapper.find('input[value="Convert to SRT"]').exists()).toBe(true)
  })

  it("disables Record Audio button when recording", async () => {
    const recordButton = findButtonByText(wrapper, "Record Audio")
    expect(recordButton.attributes("disabled")).toBeFalsy()
    await recordButton.trigger("click")
    expect(recordButton.attributes("disabled")).toBeDefined()
  })

  it("enables Stop Recording button when recording", async () => {
    const recordButton = findButtonByText(wrapper, "Record Audio")
    const stopButton = findButtonByText(wrapper, "Stop Recording")

    expect(stopButton.attributes("disabled")).toBeDefined()

    await recordButton.trigger("click")
    expect(stopButton.attributes("disabled")).toBeUndefined()
  })

  it("calls convertToSRT when Convert to SRT button is clicked", async () => {
    const convertButton = wrapper.find('input[value="Convert to SRT"]')
    const mockConvertSrt = vi
      .fn()
      .mockResolvedValue({ textFromAudio: "Converted text" })
    helper.managedApi.restAiAudioController.convertSrt = mockConvertSrt

    await convertButton.trigger("click")
    await flushPromises()

    expect(mockConvertSrt).toHaveBeenCalled()
  })

  it("handles errors when converting to SRT", async () => {
    const convertButton = wrapper.find('input[value="Convert to SRT"]')
    const mockError = { error: "Conversion failed" }
    helper.managedApi.restAiAudioController.convertSrt = vi
      .fn()
      .mockRejectedValue(mockError)

    await convertButton.trigger("click")
    await flushPromises()

    expect(wrapper.vm.noteFormErrors).toEqual(mockError)
  })

  it("starts recording when Record Audio button is clicked", async () => {
    const recordButton = findButtonByText(wrapper, "Record Audio")

    await recordButton.trigger("click")
    await flushPromises()

    expect(mockMediaDevices.getUserMedia).toHaveBeenCalledWith({ audio: true })
    expect(mockMediaStreamSource.connect).toHaveBeenCalledWith(
      mockScriptProcessor
    )
    expect(mockScriptProcessor.connect).toHaveBeenCalledWith(
      mockAudioContext.destination
    )
    expect(wrapper.vm.isRecording).toBe(true)
  })

  it("stops recording when Stop Recording button is clicked", async () => {
    // First, start recording
    await findButtonByText(wrapper, "Record Audio").trigger("click")
    await flushPromises()

    const stopButton = findButtonByText(wrapper, "Stop Recording")
    await stopButton.trigger("click")
    await flushPromises()

    expect(mockScriptProcessor.disconnect).toHaveBeenCalled()
    expect(mockMediaStreamSource.disconnect).toHaveBeenCalled()
    expect(wrapper.vm.isRecording).toBe(false)
  })

  it("stops browser recording and resets audio context when Stop Recording button is clicked", async () => {
    const mockTrackStop = vi.fn()
    mockMediaDevices.getUserMedia.mockResolvedValue({
      getTracks: () => [{ stop: mockTrackStop }],
    })

    // Start recording
    await findButtonByText(wrapper, "Record Audio").trigger("click")
    await flushPromises()

    // Stop recording
    const stopButton = findButtonByText(wrapper, "Stop Recording")
    await stopButton.trigger("click")
    await flushPromises()

    // Check if all tracks in the media stream were stopped
    expect(mockTrackStop).toHaveBeenCalled()

    // Check if isRecording is set to false
    expect(wrapper.vm.isRecording).toBe(false)

    // Check if audio context is reset
    await findButtonByText(wrapper, "Record Audio").trigger("click")
    await flushPromises()
    expect(mockMediaStreamSource.connect).toHaveBeenCalledTimes(2)
  })
})

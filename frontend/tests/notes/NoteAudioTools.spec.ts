import { flushPromises } from "@vue/test-utils"
import NoteAudioTools from "@/components/notes/accessory/NoteAudioTools.vue"
import helper from "../helpers"
import { vi } from "vitest"

// Mock MediaRecorder
const mockStart = vi.fn()
const mockStop = vi.fn()
const mockOndataavailable = vi.fn()
const mockOnstop = vi.fn()

class MockMediaRecorder {
  start = mockStart
  stop = mockStop
  ondataavailable = mockOndataavailable
  onstop = mockOnstop
  state = "active"
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
Object.defineProperty(global, "MediaRecorder", {
  writable: true,
  value: MockMediaRecorder,
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

    // Reset mocks before each test
    mockMediaDevices.getUserMedia.mockClear()
    mockStart.mockClear()
    mockStop.mockClear()
    mockOndataavailable.mockClear()
    mockOnstop.mockClear()
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
    expect(mockStart).toHaveBeenCalled()
    expect(wrapper.vm.isRecording).toBe(true)
  })

  it("stops recording when Stop Recording button is clicked", async () => {
    // First, start recording
    await findButtonByText(wrapper, "Record Audio").trigger("click")
    await flushPromises()

    const stopButton = findButtonByText(wrapper, "Stop Recording")
    await stopButton.trigger("click")
    await flushPromises()

    expect(mockStop).toHaveBeenCalled()
    expect(wrapper.vm.isRecording).toBe(false)
  })

  it("stops browser recording and resets mediaRecorder when Stop Recording button is clicked", async () => {
    // Mock the MediaRecorder stream
    const mockTrackStop = vi.fn()
    const mockStream = {
      getTracks: () => [{ stop: mockTrackStop }],
    }

    // Override the MockMediaRecorder to include the stream
    class MockMediaRecorderWithStream extends MockMediaRecorder {
      stream = mockStream
    }

    // Apply the new mock
    Object.defineProperty(global, "MediaRecorder", {
      writable: true,
      value: MockMediaRecorderWithStream,
    })

    // Start recording
    await findButtonByText(wrapper, "Record Audio").trigger("click")
    await flushPromises()

    // Stop recording
    const stopButton = findButtonByText(wrapper, "Stop Recording")
    await stopButton.trigger("click")
    await flushPromises()

    // Check if MediaRecorder.stop() was called
    expect(mockStop).toHaveBeenCalled()

    // Check if all tracks in the media stream were stopped
    expect(mockTrackStop).toHaveBeenCalled()

    // Check if isRecording is set to false
    expect(wrapper.vm.isRecording).toBe(false)

    // Check if mediaRecorder is reset to null
    // Note: We can't directly access mediaRecorder as it's not exposed,
    // but we can infer its state by trying to stop recording again
    await stopButton.trigger("click")
    expect(mockStop).toHaveBeenCalledTimes(1) // Should still be 1, not 2
  })
})

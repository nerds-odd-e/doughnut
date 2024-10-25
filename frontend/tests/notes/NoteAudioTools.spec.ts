import { flushPromises } from "@vue/test-utils"
import NoteAudioTools from "@/components/notes/accessory/NoteAudioTools.vue"
import helper from "../helpers"
import { vi } from "vitest"

const mockMediaStreamSource = {
  connect: vi.fn(),
  disconnect: vi.fn(),
}

const mockAudioWorklet = {
  addModule: vi.fn(),
}

const mockAudioContext = {
  createMediaStreamSource: () => mockMediaStreamSource,
  audioWorklet: mockAudioWorklet,
  destination: {},
}

const mockAudioWorkletNode = {
  connect: vi.fn(),
  disconnect: vi.fn(),
  port: {
    onmessage: null,
    postMessage: vi.fn(),
  },
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

// Apply mocks to global object
Object.defineProperty(global, "AudioWorkletNode", {
  writable: true,
  value: vi.fn(() => mockAudioWorkletNode),
})

Object.defineProperty(global.navigator, "mediaDevices", {
  value: mockMediaDevices,
  writable: true,
})

// Mock URL.createObjectURL
global.URL.createObjectURL = vi.fn(() => "blob:mocked-url")

// Mock getAudioRecordingWorkerURL
vi.mock("@/models/audio/recorderWorklet", () => ({
  getAudioRecordingWorkerURL: vi.fn(() => "mocked-worker-url"),
}))

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
    mockAudioWorklet.addModule.mockClear()
    mockAudioWorkletNode.connect.mockClear()
    mockAudioWorkletNode.disconnect.mockClear()
    mockAudioWorkletNode.port.postMessage.mockClear()
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
  })

  it("disables Record Audio button when recording", async () => {
    const recordButton = findButtonByText(wrapper, "Record Audio")
    expect(recordButton.attributes("disabled")).toBeFalsy()
    await recordButton.trigger("click")
    await flushPromises()
    expect(recordButton.attributes("disabled")).toBeDefined()
  })

  it("enables Stop Recording button when recording", async () => {
    const recordButton = findButtonByText(wrapper, "Record Audio")
    const stopButton = findButtonByText(wrapper, "Stop Recording")

    expect(stopButton.attributes("disabled")).toBeDefined()

    await recordButton.trigger("click")
    await flushPromises()
    expect(stopButton.attributes("disabled")).toBeUndefined()
  })

  it("starts recording when Record Audio button is clicked", async () => {
    const recordButton = findButtonByText(wrapper, "Record Audio")

    await recordButton.trigger("click")
    await flushPromises()

    expect(mockMediaDevices.getUserMedia).toHaveBeenCalledWith({ audio: true })
    expect(mockMediaStreamSource.connect).toHaveBeenCalledWith(
      mockAudioWorkletNode
    )
    expect(mockAudioWorkletNode.connect).toHaveBeenCalledWith(
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

    expect(mockAudioWorkletNode.disconnect).toHaveBeenCalled()
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

  it("renders Save Audio Locally button", () => {
    expect(findButtonByText(wrapper, "Save Audio Locally")).toBeTruthy()
  })

  it("disables Save Audio Locally button when recording", async () => {
    const saveButton = findButtonByText(wrapper, "Save Audio Locally")
    expect(saveButton.attributes("disabled")).toBeDefined()

    await findButtonByText(wrapper, "Record Audio").trigger("click")
    await flushPromises()

    expect(saveButton.attributes("disabled")).toBeDefined()
  })

  it("enables Save Audio Locally button when not recording and audio file exists", async () => {
    const saveButton = findButtonByText(wrapper, "Save Audio Locally")
    expect(saveButton.attributes("disabled")).toBeDefined()

    // Simulate recording and stopping
    await findButtonByText(wrapper, "Record Audio").trigger("click")
    await flushPromises()
    await findButtonByText(wrapper, "Stop Recording").trigger("click")
    await flushPromises()

    // Mock the existence of an audio file
    wrapper.vm.formData.uploadAudioFile = new File([], "test.webm")

    await wrapper.vm.$nextTick()
    expect(saveButton.attributes("disabled")).toBeUndefined()
  })

  it("calls saveAudioLocally when Save Audio Locally button is clicked", async () => {
    const saveButton = findButtonByText(wrapper, "Save Audio Locally")

    // Mock the existence of an audio file
    wrapper.vm.formData.uploadAudioFile = new File([], "test.webm")
    await wrapper.vm.$nextTick()

    const mockCreateObjectURL = vi.fn(() => "blob:mocked-url")
    const mockRevokeObjectURL = vi.fn()
    global.URL.createObjectURL = mockCreateObjectURL
    global.URL.revokeObjectURL = mockRevokeObjectURL

    const mockAppendChild = vi.fn()
    const mockRemoveChild = vi.fn()
    const mockClick = vi.fn()
    document.body.appendChild = mockAppendChild
    document.body.removeChild = mockRemoveChild
    HTMLAnchorElement.prototype.click = mockClick

    await saveButton.trigger("click")

    expect(mockCreateObjectURL).toHaveBeenCalledWith(
      wrapper.vm.formData.uploadAudioFile
    )
    expect(mockAppendChild).toHaveBeenCalled()
    expect(mockClick).toHaveBeenCalled()
    expect(mockRemoveChild).toHaveBeenCalled()
    expect(mockRevokeObjectURL).toHaveBeenCalledWith("blob:mocked-url")
  })
})

import { flushPromises } from "@vue/test-utils"
import NoteAudioTools from "@/components/notes/accessory/NoteAudioTools.vue"
import helper from "@tests/helpers"
import { vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import type { TextFromAudioWithCallInfo } from "@/generated/backend"
import type { AudioChunk } from "@/models/audio/audioProcessingScheduler"
import FullScreen from "@/components/common/FullScreen.vue"

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

const mockMediaStop = vi.fn()

// Mock navigator.mediaDevices
const mockMediaDevices = {
  callback: null,
  getUserMedia: vi.fn().mockResolvedValue({
    getTracks: () => [
      {
        stop: mockMediaStop,
      },
    ],
    getAudioTracks: () => [
      {
        getSettings: () => ({ deviceId: "device1" }),
      },
    ],
  }),
  enumerateDevices: vi.fn(),
  addEventListener(callback) {
    this.callback = callback
  },
}

// Add mock for enumerateDevices
const mockDevices = [
  { deviceId: "device1", kind: "audioinput", label: "Microphone 1" },
  { deviceId: "device2", kind: "audioinput", label: "Microphone 2" },
]

mockMediaDevices.enumerateDevices = vi.fn().mockResolvedValue(mockDevices)

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

const findButtonByTitle = (wrapper, title: string) => {
  return wrapper
    .findAll("button")
    .find((button) => button.attributes("title") === title)
}

describe("NoteAudioTools", () => {
  let wrapper
  const note = makeMe.aNote.please()

  beforeEach(() => {
    vi.useFakeTimers()
    // Mock the canvas element
    const mockContext = {
      drawImage: vi.fn(),
      fillRect: vi.fn(),
      fillStyle: "",
    }
    vi.spyOn(HTMLCanvasElement.prototype, "getContext").mockReturnValue(
      mockContext as unknown as CanvasRenderingContext2D
    )

    wrapper = helper
      .component(NoteAudioTools)
      .withStorageProps({
        note,
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
    mockMediaDevices.enumerateDevices.mockClear()
    mockMediaStop.mockClear()
  })

  it("renders the component with correct buttons", () => {
    expect(findButtonByTitle(wrapper, "Record Audio")).toBeTruthy()
    expect(findButtonByTitle(wrapper, "Stop Recording")).toBeTruthy()
  })

  it("replace Record Audio button when recording", async () => {
    const recordButtonBefore = findButtonByTitle(wrapper, "Record Audio")
    expect(recordButtonBefore.attributes("disabled")).toBeFalsy()
    await recordButtonBefore.trigger("click")
    await flushPromises()
    const recordButtonAfter = findButtonByTitle(wrapper, "Record Audio")
    expect(recordButtonAfter).toBeUndefined()
  })

  it("enables Stop Recording button when recording", async () => {
    const recordButton = findButtonByTitle(wrapper, "Record Audio")
    const stopButton = findButtonByTitle(wrapper, "Stop Recording")

    expect(stopButton.attributes("disabled")).toBeDefined()

    await recordButton.trigger("click")
    await flushPromises()
    expect(stopButton.attributes("disabled")).toBeUndefined()
  })

  it("starts recording when Record Audio button is clicked", async () => {
    const recordButton = findButtonByTitle(wrapper, "Record Audio")

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
    await findButtonByTitle(wrapper, "Record Audio").trigger("click")
    await flushPromises()

    const stopButton = findButtonByTitle(wrapper, "Stop Recording")
    await stopButton.trigger("click")
    await flushPromises()

    expect(mockAudioWorkletNode.disconnect).toHaveBeenCalled()
    expect(mockMediaStreamSource.disconnect).toHaveBeenCalled()
    expect(wrapper.vm.isRecording).toBe(false)
  })

  it("stops browser recording and resets audio context when Stop Recording button is clicked", async () => {
    // Start recording
    await findButtonByTitle(wrapper, "Record Audio").trigger("click")
    await flushPromises()

    // Stop recording
    const stopButton = findButtonByTitle(wrapper, "Stop Recording")
    await stopButton.trigger("click")
    await flushPromises()

    // Check if all tracks in the media stream were stopped
    expect(mockMediaStop).toHaveBeenCalled()

    // Check if isRecording is set to false
    expect(wrapper.vm.isRecording).toBe(false)

    // Check if audio context is reset
    await findButtonByTitle(wrapper, "Record Audio").trigger("click")
    await flushPromises()
    expect(mockMediaStreamSource.connect).toHaveBeenCalledTimes(2)
  })

  it("renders Save Audio Locally button", () => {
    expect(findButtonByTitle(wrapper, "Save Audio Locally")).toBeTruthy()
  })

  it("disables Save Audio Locally button when recording", async () => {
    const saveButton = findButtonByTitle(wrapper, "Save Audio Locally")
    expect(saveButton.attributes("disabled")).toBeDefined()

    await findButtonByTitle(wrapper, "Record Audio").trigger("click")
    await flushPromises()

    expect(saveButton.attributes("disabled")).toBeDefined()
  })

  it("enables Save Audio Locally button when not recording and audio file exists", async () => {
    const saveButton = findButtonByTitle(wrapper, "Save Audio Locally")
    expect(saveButton.attributes("disabled")).toBeDefined()

    // Simulate recording and stopping
    await findButtonByTitle(wrapper, "Record Audio").trigger("click")
    await flushPromises()
    await findButtonByTitle(wrapper, "Stop Recording").trigger("click")
    await flushPromises()

    // Mock the existence of an audio file
    wrapper.vm.audioFile = new File([], "test.webm")

    await wrapper.vm.$nextTick()
    expect(saveButton.attributes("disabled")).toBeUndefined()
  })

  it("calls saveAudioLocally when Save Audio Locally button is clicked", async () => {
    const saveButton = findButtonByTitle(wrapper, "Save Audio Locally")

    // Mock the existence of an audio file
    wrapper.vm.audioFile = new File([], "test.webm")
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

    expect(mockCreateObjectURL).toHaveBeenCalledWith(wrapper.vm.audioFile)
    expect(mockAppendChild).toHaveBeenCalled()
    expect(mockClick).toHaveBeenCalled()
    expect(mockRemoveChild).toHaveBeenCalled()
    expect(mockRevokeObjectURL).toHaveBeenCalledWith("blob:mocked-url")
  })

  it("renders close button and emits closeDialog event when clicked", async () => {
    const closeButton = wrapper.find(".close-btn")
    expect(closeButton.exists()).toBe(true)

    await closeButton.trigger("click")
    expect(wrapper.emitted().closeDialog).toBeTruthy()
  })

  it("stops recording and emits closeDialog event when close button is clicked while recording", async () => {
    // Start recording
    await findButtonByTitle(wrapper, "Record Audio").trigger("click")
    await flushPromises()

    const closeButton = wrapper.find(".close-btn")
    await closeButton.trigger("click")

    expect(wrapper.vm.isRecording).toBe(false)
    expect(mockAudioWorkletNode.disconnect).toHaveBeenCalled()
    expect(mockMediaStreamSource.disconnect).toHaveBeenCalled()
    expect(wrapper.emitted().closeDialog).toBeTruthy()
  })

  it("renders Flush Audio button", () => {
    expect(findButtonByTitle(wrapper, "Flush Audio")).toBeTruthy()
  })

  it("disables Flush Audio button when not recording", () => {
    const flushButton = findButtonByTitle(wrapper, "Flush Audio")
    expect(flushButton.attributes("disabled")).toBeDefined()
  })

  it("enables Flush Audio button when recording", async () => {
    const recordButton = findButtonByTitle(wrapper, "Record Audio")
    const flushButton = findButtonByTitle(wrapper, "Flush Audio")

    expect(flushButton.attributes("disabled")).toBeDefined()

    await recordButton.trigger("click")
    await flushPromises()
    expect(flushButton.attributes("disabled")).toBeUndefined()
  })

  it("calls audioRecorder.tryFlush when Flush Audio button is clicked", async () => {
    const mockFlush = vi.fn()
    wrapper.vm.audioRecorder.tryFlush = mockFlush

    // Start recording
    await findButtonByTitle(wrapper, "Record Audio").trigger("click")
    await flushPromises()

    const flushButton = findButtonByTitle(wrapper, "Flush Audio")
    await flushButton.trigger("click")

    expect(mockFlush).toHaveBeenCalled()
  })

  it("disables Flush Audio button during audio processing", async () => {
    // Start recording
    await findButtonByTitle(wrapper, "Record Audio").trigger("click")
    await flushPromises()

    const flushButton = findButtonByTitle(wrapper, "Flush Audio")

    // Mock a long-running audio processing
    let resolveProcess
    const processPromise = new Promise((resolve) => {
      resolveProcess = resolve
    })
    const audioToTextForNoteMock = vi.fn()
    helper.managedApi.restAiAudioController.audioToTextForNote =
      audioToTextForNoteMock
    audioToTextForNoteMock.mockReturnValue(processPromise)

    // Trigger audio processing
    const processPromise2 = wrapper.vm.processAudio(new Blob())
    await flushPromises()

    // Button should be disabled during processing
    expect(flushButton.attributes("disabled")).toBeDefined()

    // Resolve the processing
    resolveProcess!({ completionFromAudio: "test" })
    await processPromise2
    await flushPromises()

    // Button should be enabled again after processing (when recording)
    expect(flushButton.attributes("disabled")).toBeUndefined()
  })

  describe("Audio Device Selection", () => {
    it("loads audio devices when recording starts", async () => {
      const recordButton = findButtonByTitle(wrapper, "Record Audio")
      await recordButton.trigger("click")
      await flushPromises()

      expect(mockMediaDevices.enumerateDevices).toHaveBeenCalled()
      const deviceSelect = wrapper.find(".device-select")
      expect(deviceSelect.exists()).toBe(true)
      expect(deviceSelect.findAll("option").length).toBe(mockDevices.length)
    })

    it("switches audio device when selection changes", async () => {
      // Start recording
      await findButtonByTitle(wrapper, "Record Audio").trigger("click")
      await flushPromises()

      const deviceSelect = wrapper.find(".device-select")
      await deviceSelect.setValue("device2")
      await flushPromises()

      // Verify that getUserMedia was called with the new device ID
      expect(mockMediaDevices.getUserMedia).toHaveBeenCalledWith({
        audio: { deviceId: { exact: "device2" } },
      })
    })
  })

  describe("Topic suggestion", () => {
    beforeEach(() => {
      // Reset mocks and wrapper before each test
      vi.clearAllMocks()
      helper.managedApi.restTextContentController.updateNoteTopicConstructor =
        vi.fn()
      helper.managedApi.restAiAudioController.audioToText = vi
        .fn()
        .mockResolvedValue({ completionFromAudio: "text" })
    })

    it("suggests topic for power-of-2 audio processes", async () => {
      const note = makeMe.aNote.topicConstructor("Untitled").please()
      wrapper = helper
        .component(NoteAudioTools)
        .withStorageProps({ note })
        .mount()

      helper.managedApi.restAiController.suggestTopicTitle = vi
        .fn()
        .mockResolvedValue({ topic: "Suggested Topic" })

      // Simulate 9 audio processes (should trigger on 1st, 2nd, 4th, 8th calls)
      for (let i = 0; i < 9; i++) {
        await wrapper.vm.processAudio(new Blob())
      }

      // Should call suggestTopicTitle 4 times (on calls 1, 2, 4, and 8)
      expect(
        helper.managedApi.restAiController.suggestTopicTitle
      ).toHaveBeenCalledTimes(4)
      expect(
        helper.managedApi.restTextContentController.updateNoteTopicConstructor
      ).toHaveBeenCalledTimes(4)
    })

    it("does not update topic when suggestion is empty", async () => {
      const note = makeMe.aNote.topicConstructor("Untitled").please()
      wrapper = helper
        .component(NoteAudioTools)
        .withStorageProps({ note })
        .mount()

      helper.managedApi.restAiController.suggestTopicTitle = vi
        .fn()
        .mockResolvedValue({ topic: "" })

      await wrapper.vm.processAudio(new Blob())

      expect(
        helper.managedApi.restAiController.suggestTopicTitle
      ).toHaveBeenCalled()
      expect(
        helper.managedApi.restTextContentController.updateNoteTopicConstructor
      ).not.toHaveBeenCalled()
    })
  })

  describe("Audio processing with thread context", () => {
    let audioToTextForNoteMock
    beforeEach(() => {
      audioToTextForNoteMock = vi.fn()
      helper.managedApi.restAiAudioController.audioToTextForNote =
        audioToTextForNoteMock
    })

    it("stores and reuses thread context between calls", async () => {
      const mockResponse1: TextFromAudioWithCallInfo = {
        completionFromAudio: { completion: "text1", deleteFromEnd: 0 },
        toolCallInfo: {
          threadId: "thread-123",
          runId: "run-123",
          toolCallId: "tool-123",
        },
      }

      const mockResponse2: TextFromAudioWithCallInfo = {
        completionFromAudio: { completion: "text2", deleteFromEnd: 0 },
        toolCallInfo: {
          threadId: "thread-123",
          runId: "run-124",
          toolCallId: "tool-124",
        },
      }

      audioToTextForNoteMock
        .mockResolvedValueOnce(mockResponse1)
        .mockResolvedValueOnce(mockResponse2)

      // First call
      await wrapper.vm.processAudio(new Blob())

      expect(
        helper.managedApi.restAiAudioController.audioToTextForNote
      ).toHaveBeenLastCalledWith(
        expect.any(Number),
        expect.objectContaining({
          threadId: undefined,
          runId: undefined,
          toolCallId: undefined,
        })
      )

      // Second call should include previous thread context
      await wrapper.vm.processAudio(new Blob())

      expect(
        helper.managedApi.restAiAudioController.audioToTextForNote
      ).toHaveBeenLastCalledWith(
        expect.any(Number),
        expect.objectContaining({
          threadId: "thread-123",
          runId: "run-123",
          toolCallId: "tool-123",
        })
      )
    })

    it("maintains thread context even after errors", async () => {
      const mockResponse: TextFromAudioWithCallInfo = {
        completionFromAudio: { completion: "text1", deleteFromEnd: 0 },
        toolCallInfo: {
          threadId: "thread-123",
          runId: "run-123",
          toolCallId: "tool-123",
        },
      }

      audioToTextForNoteMock
        .mockResolvedValueOnce(mockResponse)
        .mockRejectedValueOnce(new Error("API Error"))
        .mockResolvedValueOnce(mockResponse)

      // First successful call
      await wrapper.vm.processAudio(new Blob())

      // Failed call
      await wrapper.vm.processAudio(new Blob())

      // Third call should still have thread context
      await wrapper.vm.processAudio(new Blob())

      const lastCall = audioToTextForNoteMock.mock.calls.pop()
      expect(lastCall[1]).toMatchObject({
        threadId: "thread-123",
        runId: "run-123",
        toolCallId: "tool-123",
      })
    })
  })

  describe("Advanced Options", () => {
    let audioToTextForNoteMock
    beforeEach(() => {
      audioToTextForNoteMock = vi.fn()
      helper.managedApi.restAiAudioController.audioToTextForNote =
        audioToTextForNoteMock
    })

    it("shows advanced options when toggle button is clicked", async () => {
      const advancedButton = findButtonByTitle(wrapper, "Advanced Options")
      expect(wrapper.find(".advanced-options").exists()).toBe(false)

      await advancedButton.trigger("click")
      expect(wrapper.find(".advanced-options").exists()).toBe(true)

      await advancedButton.trigger("click")
      expect(wrapper.find(".advanced-options").exists()).toBe(false)
    })

    it("includes processing instructions in API call", async () => {
      const advancedButton = findButtonByTitle(wrapper, "Advanced Options")
      await advancedButton.trigger("click")

      const input = wrapper.find("#processingInstructions")
      await input.setValue("Test instructions")

      const testBlob = new Blob(["test"])
      await wrapper.vm.processAudio(testBlob)

      expect(
        helper.managedApi.restAiAudioController.audioToTextForNote
      ).toHaveBeenCalledWith(
        expect.any(Number),
        expect.objectContaining({
          additionalProcessingInstructions: "Test instructions",
        })
      )
    })

    it("maintains processing instructions between recordings", async () => {
      const advancedButton = findButtonByTitle(wrapper, "Advanced Options")
      await advancedButton.trigger("click")

      const input = wrapper.find("#processingInstructions")
      await input.setValue("Test instructions")

      // First recording
      const testBlob1 = new Blob(["test1"])
      await wrapper.vm.processAudio(testBlob1)

      // Second recording
      const testBlob2 = new Blob(["test2"])
      await wrapper.vm.processAudio(testBlob2)

      const calls = audioToTextForNoteMock.mock.calls
      expect(calls[0][1].additionalProcessingInstructions).toBe(
        "Test instructions"
      )
      expect(calls[1][1].additionalProcessingInstructions).toBe(
        "Test instructions"
      )
    })
  })

  describe("Audio processing with isMidSpeech flag", () => {
    let audioToTextForNoteMock: ReturnType<typeof vi.fn>

    beforeEach(() => {
      audioToTextForNoteMock = vi.fn().mockResolvedValue({
        completionFromAudio: "text",
        toolCallInfo: {
          threadId: "thread-123",
          runId: "run-123",
          toolCallId: "tool-123",
        },
      })
      helper.managedApi.restAiAudioController.audioToTextForNote =
        audioToTextForNoteMock
    })

    it("should pass isMidSpeech=true when processing timer-triggered chunk", async () => {
      const testBlob2 = new Blob(["test2"])
      await wrapper.vm.processAudio(<AudioChunk>{
        data: testBlob2,
        isMidSpeech: true,
      })
      await flushPromises()

      expect(audioToTextForNoteMock).toHaveBeenCalledWith(
        expect.any(Number),
        expect.objectContaining({
          isMidSpeech: true,
        })
      )
    })
  })

  it("should handle returned timestamp from audio processing", async () => {
    const mockResponse = {
      completionFromAudio: "text",
      endTimestamp: "00:00:37,270",
      toolCallInfo: {
        threadId: "thread-123",
        runId: "run-123",
        toolCallId: "tool-123",
      },
    }

    helper.managedApi.restAiAudioController.audioToTextForNote = vi
      .fn()
      .mockResolvedValue(mockResponse)

    const testBlob = new Blob(["test"])
    const result = await wrapper.vm.processAudio({
      data: testBlob,
      isMidSpeech: true,
    })

    expect(result).toBe("00:00:37,270")
  })

  describe("Fullscreen Integration", () => {
    beforeEach(() => {
      document.body.innerHTML = ""

      // Mock document fullscreen methods
      document.documentElement.requestFullscreen = vi
        .fn()
        .mockResolvedValue(undefined)
      document.exitFullscreen = vi.fn().mockResolvedValue(undefined)
      document.exitPointerLock = vi.fn()
      document.documentElement.requestPointerLock = vi.fn()

      // Mock fullscreenElement
      Object.defineProperty(document, "fullscreenElement", {
        configurable: true,
        get: () => document.documentElement,
      })
    })

    afterEach(() => {
      document.body.innerHTML = ""
    })

    it("shows FullScreen component in advanced options", async () => {
      const advancedButton = findButtonByTitle(wrapper, "Advanced Options")
      await advancedButton.trigger("click")

      expect(wrapper.findComponent(FullScreen).exists()).toBe(true)
    })

    it("displays error message in fullscreen overlay when errors exist", async () => {
      wrapper = helper
        .component(NoteAudioTools)
        .withStorageProps({ note })
        .mount({ attachTo: document.body })

      const advancedButton = findButtonByTitle(wrapper, "Advanced Options")
      await advancedButton.trigger("click")

      // Set an error
      wrapper.vm.errors = { someError: "Test error message" }
      await wrapper.vm.$nextTick()

      const fullscreenComponent = wrapper.findComponent(FullScreen)
      await fullscreenComponent.find(".fullscreen-btn").trigger("click")
      await wrapper.vm.$nextTick()

      const errorElement = document.body.querySelector(".fullscreen-error")
      expect(errorElement).toBeTruthy()
      expect(errorElement?.textContent?.trim()).toBe("Test error message")
    })
  })
})

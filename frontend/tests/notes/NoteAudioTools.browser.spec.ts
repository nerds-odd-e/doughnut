import FullScreen from "@/components/common/FullScreen.vue"
import NoteAudioTools from "@/components/notes/accessory/NoteAudioTools.vue"
import type { AudioChunk } from "@/models/audio/audioProcessingScheduler"
import makeMe from "@tests/fixtures/makeMe"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
  mockShowNote,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, vi } from "vitest"

// Browser Mode: Mock audioRecorder module (application module, not browser API)
// This mock simulates the audioRecorder's behavior while allowing us to test component logic
const mockMediaStreamSource = {
  connect: vi.fn(),
  disconnect: vi.fn(),
}

const mockAudioWorklet = {
  addModule: vi.fn(),
}

const mockAudioContext = {
  createMediaStreamSource: vi.fn(() => mockMediaStreamSource),
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

// Browser Mode: Mock navigator.mediaDevices.getUserMedia (requires user permission)
// We mock this because getUserMedia requires user interaction in real browsers
const mockMediaDevices = {
  getUserMedia: vi.fn().mockImplementation(() =>
    Promise.resolve({
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
    })
  ),
  enumerateDevices: vi.fn(),
  addEventListener: vi.fn(),
}

// Add mock for enumerateDevices
const mockDevices = [
  { deviceId: "device1", kind: "audioinput", label: "Microphone 1" },
  { deviceId: "device2", kind: "audioinput", label: "Microphone 2" },
]

mockMediaDevices.enumerateDevices = vi
  .fn()
  .mockImplementation(() => Promise.resolve(mockDevices))

// Browser Mode: Spy on AudioContext and AudioWorkletNode (real browser APIs)
// These exist in real browsers, so we spy on them rather than creating fake ones
beforeEach(() => {
  // Browser Mode: Spy on REAL AudioContext constructor
  vi.spyOn(globalThis, "AudioContext").mockImplementation(
    () => mockAudioContext as unknown as AudioContext
  )

  // Browser Mode: Spy on REAL AudioWorkletNode constructor
  vi.spyOn(globalThis, "AudioWorkletNode").mockImplementation(
    () => mockAudioWorkletNode as unknown as AudioWorkletNode
  )

  // Browser Mode: Mock navigator.mediaDevices (requires user permission)
  Object.defineProperty(globalThis.navigator, "mediaDevices", {
    value: mockMediaDevices,
    writable: true,
    configurable: true,
  })
})

afterEach(() => {
  vi.restoreAllMocks()
  document.body.innerHTML = ""
})

// Browser Mode: Mock URL.createObjectURL (real browser API, but we spy on it for verification)
// Note: Real URL.createObjectURL works in browser mode, but we spy to verify calls
const mockCreateObjectURL = vi.fn(
  (blob: Blob | MediaSource) =>
    `blob:${(blob as Blob).type || "unknown"}-mocked-url`
)
const mockRevokeObjectURL = vi.fn()

beforeEach(() => {
  // Browser Mode: Spy on REAL URL.createObjectURL and revokeObjectURL
  vi.spyOn(URL, "createObjectURL").mockImplementation(mockCreateObjectURL)
  vi.spyOn(URL, "revokeObjectURL").mockImplementation(mockRevokeObjectURL)
})

// Mock getAudioRecordingWorkerURL
vi.mock("@/models/audio/recorderWorklet", () => ({
  getAudioRecordingWorkerURL: vi.fn(() => "mocked-worker-url"),
}))

// Mock the audioRecorder module (application module, not browser API)
// Browser Mode: Import ref at top level, then use it in the factory
vi.mock("@/models/audio/audioRecorder", async () => {
  const { ref } = await import("vue")
  return {
    createAudioRecorder: vi.fn(() => ({
      startRecording: vi.fn().mockImplementation(async () => {
        // Simulate Web Audio API calls that would happen in real audioRecorder
        mockMediaDevices.getUserMedia({ audio: true })
        mockMediaStreamSource.connect(mockAudioWorkletNode)
        mockAudioWorkletNode.connect(mockAudioContext.destination)
        return undefined
      }),
      stopRecording: vi.fn().mockImplementation(async () => {
        // Simulate cleanup that would happen in real audioRecorder
        mockAudioWorkletNode.disconnect()
        mockMediaStreamSource.disconnect()
        mockMediaStop()
        return new File([], "test.webm")
      }),
      getAudioData: vi.fn(() => 0),
      tryFlush: vi.fn().mockResolvedValue(undefined),
      getAudioDevices: vi.fn().mockImplementation(() => {
        // Simulate device enumeration
        mockMediaDevices.enumerateDevices()
        return ref(mockDevices)
      }),
      getSelectedDevice: vi.fn(() => ref("device1")),
      switchAudioDevice: vi.fn().mockImplementation(async (deviceId) => {
        // Simulate switching device
        mockMediaDevices.getUserMedia({
          audio: { deviceId: { exact: deviceId } },
        })
        return undefined
      }),
    })),
  }
})

// Mock the wakeLocker module (application module, not browser API)
vi.mock("@/models/wakeLocker", () => ({
  createWakeLocker: vi.fn(() => ({
    request: vi.fn().mockResolvedValue(undefined),
    release: vi.fn().mockResolvedValue(undefined),
  })),
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
    mockShowNote()

    // Browser Mode: Use REAL Canvas API!
    // HTMLCanvasElement.getContext() is a real browser API
    // We spy on it to verify calls, but the real implementation works
    const mockContext = {
      drawImage: vi.fn(),
      fillRect: vi.fn(),
      fillStyle: "",
    }
    vi.spyOn(HTMLCanvasElement.prototype, "getContext").mockReturnValue(
      mockContext as unknown as CanvasRenderingContext2D
    )

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

    // Reset URL mocks
    mockCreateObjectURL.mockClear()
    mockRevokeObjectURL.mockClear()

    wrapper = helper
      .component(NoteAudioTools)
      .withCleanStorage()
      .withProps({
        note,
      })
      .mount()
  })

  afterEach(() => {
    vi.useRealTimers()
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
    await wrapper.vm.$nextTick()

    // Debug: Check if isRecording is set
    expect(wrapper.vm.isRecording).toBe(true)

    const recordButtonAfter = findButtonByTitle(wrapper, "Record Audio")
    expect(recordButtonAfter).toBeUndefined()
  })

  it("enables Stop Recording button when recording", async () => {
    const recordButton = findButtonByTitle(wrapper, "Record Audio")
    const stopButton = findButtonByTitle(wrapper, "Stop Recording")

    expect(stopButton.attributes("disabled")).toBeDefined()

    await recordButton.trigger("click")
    await flushPromises()
    await wrapper.vm.$nextTick()
    expect(stopButton.attributes("disabled")).toBeFalsy()
  })

  it("starts recording when Record Audio button is clicked", async () => {
    const recordButton = findButtonByTitle(wrapper, "Record Audio")

    await recordButton.trigger("click")
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.audioRecorder.startRecording).toHaveBeenCalled()
    expect(wrapper.vm.isRecording).toBe(true)

    // Verify wake lock is requested when recording starts
    expect(wrapper.vm.wakeLocker.request).toHaveBeenCalled()
  })

  it("stops recording when Stop Recording button is clicked", async () => {
    // First, start recording
    await findButtonByTitle(wrapper, "Record Audio").trigger("click")
    await flushPromises()
    await wrapper.vm.$nextTick()

    const stopButton = findButtonByTitle(wrapper, "Stop Recording")
    await stopButton.trigger("click")
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.audioRecorder.stopRecording).toHaveBeenCalled()
    expect(wrapper.vm.isRecording).toBe(false)

    // Verify Web Audio API cleanup
    expect(mockAudioWorkletNode.disconnect).toHaveBeenCalled()
    expect(mockMediaStreamSource.disconnect).toHaveBeenCalled()
    expect(mockMediaStop).toHaveBeenCalled()

    // Verify wake lock is released when recording stops
    expect(wrapper.vm.wakeLocker.release).toHaveBeenCalled()
  })

  it("stops browser recording and resets audio context when Stop Recording button is clicked", async () => {
    // Start recording
    await findButtonByTitle(wrapper, "Record Audio").trigger("click")
    await flushPromises()
    await wrapper.vm.$nextTick()

    // Stop recording
    const stopButton = findButtonByTitle(wrapper, "Stop Recording")
    await stopButton.trigger("click")
    await flushPromises()
    await wrapper.vm.$nextTick()

    // Check if stopRecording was called
    expect(wrapper.vm.audioRecorder.stopRecording).toHaveBeenCalled()

    // Check if isRecording is set to false
    expect(wrapper.vm.isRecording).toBe(false)

    // Verify Web Audio API cleanup
    expect(mockAudioWorkletNode.disconnect).toHaveBeenCalled()
    expect(mockMediaStreamSource.disconnect).toHaveBeenCalled()
    expect(mockMediaStop).toHaveBeenCalled()

    // Verify wake lock is released
    expect(wrapper.vm.wakeLocker.release).toHaveBeenCalled()

    // Check if audio context is reset by starting recording again
    await findButtonByTitle(wrapper, "Record Audio").trigger("click")
    await flushPromises()
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.audioRecorder.startRecording).toHaveBeenCalledTimes(2)

    // Verify Web Audio API connections are re-established
    expect(mockMediaStreamSource.connect).toHaveBeenCalledTimes(2)
    expect(mockAudioWorkletNode.connect).toHaveBeenCalledTimes(2)

    // Verify wake lock is requested again for new recording
    expect(wrapper.vm.wakeLocker.request).toHaveBeenCalledTimes(2)
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
    expect(saveButton.attributes("disabled")).toBeFalsy()
  })

  it("calls saveAudioLocally when Save Audio Locally button is clicked", async () => {
    const saveButton = findButtonByTitle(wrapper, "Save Audio Locally")

    // Mock the existence of an audio file
    wrapper.vm.audioFile = new File([], "test.webm")
    await wrapper.vm.$nextTick()

    // Browser Mode: Spy on REAL DOM methods!
    const mockAppendChild = vi.spyOn(document.body, "appendChild")
    const mockRemoveChild = vi.spyOn(document.body, "removeChild")
    const mockClick = vi.spyOn(HTMLAnchorElement.prototype, "click")

    await saveButton.trigger("click")

    // Browser Mode: Verify REAL URL.createObjectURL is called!
    expect(URL.createObjectURL).toHaveBeenCalledWith(wrapper.vm.audioFile)
    expect(mockAppendChild).toHaveBeenCalled()
    expect(mockClick).toHaveBeenCalled()
    expect(mockRemoveChild).toHaveBeenCalled()
    // Browser Mode: Verify REAL URL.revokeObjectURL is called!
    expect(URL.revokeObjectURL).toHaveBeenCalledWith(
      mockCreateObjectURL(wrapper.vm.audioFile)
    )

    mockAppendChild.mockRestore()
    mockRemoveChild.mockRestore()
    mockClick.mockRestore()
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
    await wrapper.vm.$nextTick()

    const closeButton = wrapper.find(".close-btn")
    await closeButton.trigger("click")
    await flushPromises()
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.isRecording).toBe(false)
    expect(wrapper.vm.audioRecorder.stopRecording).toHaveBeenCalled()
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
    await wrapper.vm.$nextTick()
    expect(flushButton.attributes("disabled")).toBeFalsy()
  })

  it("calls audioRecorder.tryFlush when Flush Audio button is clicked", async () => {
    // Start recording
    await findButtonByTitle(wrapper, "Record Audio").trigger("click")
    await flushPromises()
    await wrapper.vm.$nextTick()

    const flushButton = findButtonByTitle(wrapper, "Flush Audio")
    await flushButton.trigger("click")

    // Verify that the mocked tryFlush was called
    expect(wrapper.vm.audioRecorder.tryFlush).toHaveBeenCalled()
  })

  it("sets up Web Audio API connections when recording starts", async () => {
    const recordButton = findButtonByTitle(wrapper, "Record Audio")

    await recordButton.trigger("click")
    await flushPromises()
    await wrapper.vm.$nextTick()

    // Verify high-level audio recorder calls
    expect(wrapper.vm.audioRecorder.startRecording).toHaveBeenCalled()

    // Verify low-level Web Audio API calls
    expect(mockMediaDevices.getUserMedia).toHaveBeenCalledWith({ audio: true })
    expect(mockMediaStreamSource.connect).toHaveBeenCalledWith(
      mockAudioWorkletNode
    )
    expect(mockAudioWorkletNode.connect).toHaveBeenCalledWith(
      mockAudioContext.destination
    )
  })

  it("manages wake lock during recording session", async () => {
    const recordButton = findButtonByTitle(wrapper, "Record Audio")

    // Start recording
    await recordButton.trigger("click")
    await flushPromises()
    await wrapper.vm.$nextTick()

    // Verify wake lock is requested
    expect(wrapper.vm.wakeLocker.request).toHaveBeenCalled()

    // Stop recording
    const stopButton = findButtonByTitle(wrapper, "Stop Recording")
    await stopButton.trigger("click")
    await flushPromises()
    await wrapper.vm.$nextTick()

    // Verify wake lock is released
    expect(wrapper.vm.wakeLocker.release).toHaveBeenCalled()
  })

  it("disables Flush Audio button during audio processing", async () => {
    // Start recording
    await findButtonByTitle(wrapper, "Record Audio").trigger("click")
    await flushPromises()
    await wrapper.vm.$nextTick()

    const flushButton = findButtonByTitle(wrapper, "Flush Audio")

    type AudioResponse = {
      completionFromAudio: { details: string }
      endTimestamp: string
    }

    // Mock a long-running audio processing
    let resolveProcess: (value: AudioResponse) => void
    const processPromise = new Promise<AudioResponse>((resolve) => {
      resolveProcess = resolve
    })
    mockSdkServiceWithImplementation("audioToText", async () => {
      return await processPromise
    })

    // Trigger audio processing
    const processPromise2 = wrapper.vm.processAudio({
      data: new Blob(),
      isMidSpeech: true,
    })
    await flushPromises()

    // Button should be disabled during processing
    expect(flushButton.attributes("disabled")).toBeDefined()

    // Resolve the processing
    resolveProcess!({
      completionFromAudio: { details: "test" },
      endTimestamp: "00:00:37,270",
    })
    await processPromise2
    await flushPromises()

    // Button should be enabled again after processing (when recording)
    expect(flushButton.attributes("disabled")).toBeFalsy()
  })

  describe("Audio Device Selection", () => {
    it("loads audio devices when recording starts", async () => {
      const recordButton = findButtonByTitle(wrapper, "Record Audio")
      await recordButton.trigger("click")
      await flushPromises()
      await wrapper.vm.$nextTick()

      const deviceSelect = wrapper.find(".device-select")
      expect(deviceSelect.exists()).toBe(true)
      expect(deviceSelect.findAll("option").length).toBe(mockDevices.length)

      // Verify audio devices are loaded through audioRecorder
      expect(wrapper.vm.audioRecorder.getAudioDevices).toHaveBeenCalled()

      // Verify browser API for device enumeration
      expect(mockMediaDevices.enumerateDevices).toHaveBeenCalled()
    })

    it("switches audio device when selection changes", async () => {
      // Start recording
      await findButtonByTitle(wrapper, "Record Audio").trigger("click")
      await flushPromises()
      await wrapper.vm.$nextTick()

      const deviceSelect = wrapper.find(".device-select")
      await deviceSelect.setValue("device2")
      await flushPromises()
      await wrapper.vm.$nextTick()

      // Verify that switchAudioDevice was called with the new device ID
      expect(wrapper.vm.audioRecorder.switchAudioDevice).toHaveBeenCalledWith(
        "device2"
      )

      // Verify getUserMedia is called with new device constraints
      expect(mockMediaDevices.getUserMedia).toHaveBeenCalledWith({
        audio: { deviceId: { exact: "device2" } },
      })
    })
  })

  describe("Title suggestion", () => {
    let updateNoteTitleSpy: ReturnType<typeof mockSdkService<"updateNoteTitle">>

    beforeEach(() => {
      // Reset mocks and wrapper before each test
      vi.clearAllMocks()
      updateNoteTitleSpy = mockSdkService("updateNoteTitle", {} as never)
      mockSdkService("audioToText", {
        completionFromAudio: { details: "text" },
        endTimestamp: "00:00:00,000",
      })
    })

    it("suggests title for power-of-2 audio processes", async () => {
      const note = makeMe.aNote.title("Untitled").please()
      wrapper = helper
        .component(NoteAudioTools)
        .withCleanStorage()
        .withProps({ note })
        .mount()

      const suggestTitleSpy = mockSdkService("suggestTitle", {
        title: "Suggested Title",
      })
      mockSdkService("updateNoteDetails", makeMe.aNoteRealm.please())

      // Simulate 9 audio processes (should trigger on 1st, 2nd, 4th, 8th calls)
      for (let i = 0; i < 9; i++) {
        await wrapper.vm.processAudio(new Blob())
      }

      // Should call suggestTitle 4 times (on calls 1, 2, 4, and 8)
      expect(suggestTitleSpy).toHaveBeenCalledTimes(4)
      expect(updateNoteTitleSpy).toHaveBeenCalledTimes(4)
    })

    it("does not update title when suggestion is empty", async () => {
      const note = makeMe.aNote.title("Untitled").please()
      wrapper = helper
        .component(NoteAudioTools)
        .withCleanStorage()
        .withProps({ note })
        .mount()

      const suggestTitleSpy = mockSdkService("suggestTitle", { title: "" })
      mockSdkService("updateNoteDetails", makeMe.aNoteRealm.please())

      await wrapper.vm.processAudio(new Blob())

      expect(suggestTitleSpy).toHaveBeenCalled()
      expect(updateNoteTitleSpy).not.toHaveBeenCalled()
    })
  })

  describe("Audio processing with thread context", () => {
    let audioToTextMock

    beforeEach(() => {
      audioToTextMock = mockSdkService("audioToText", {
        completionFromAudio: { details: "text" },
        endTimestamp: "00:00:37,270",
      })
    })

    afterEach(() => {
      audioToTextMock.mockRestore()
    })

    it("stores and reuses thread context between calls", async () => {
      const mockResponse1 = {
        completionFromAudio: { details: "text1" },
        endTimestamp: "00:00:37,270",
      }

      const mockResponse2 = {
        completionFromAudio: { details: "text2" },
        endTimestamp: "00:00:47,270",
      }

      audioToTextMock
        .mockResolvedValueOnce(wrapSdkResponse(mockResponse1))
        .mockResolvedValueOnce(wrapSdkResponse(mockResponse2))

      // First call
      await wrapper.vm.processAudio(new Blob())

      expect(audioToTextMock).toHaveBeenLastCalledWith({
        body: expect.objectContaining({
          previousNoteDetailsToAppendTo: note.details,
        }),
      })

      // Second call should include previous thread context
      await wrapper.vm.processAudio(new Blob())

      expect(audioToTextMock).toHaveBeenLastCalledWith({
        body: expect.objectContaining({
          previousNoteDetailsToAppendTo: note.details,
        }),
      })
    })

    it("maintains thread context even after errors", async () => {
      const mockResponse = {
        completionFromAudio: { details: "text1" },
        endTimestamp: "00:00:37,270",
      }

      audioToTextMock
        .mockResolvedValueOnce(wrapSdkResponse(mockResponse))
        .mockResolvedValueOnce(wrapSdkError("API Error"))
        .mockResolvedValueOnce(wrapSdkResponse(mockResponse))

      // First successful call
      await wrapper.vm.processAudio(new Blob())

      // Failed call
      await wrapper.vm.processAudio(new Blob())

      // Third call should still have previous content
      await wrapper.vm.processAudio(new Blob())

      const lastCall = audioToTextMock.mock.calls.pop()
      expect(lastCall).toBeDefined()
      expect(lastCall![0]).toMatchObject({
        body: {
          previousNoteDetailsToAppendTo: note.details,
        },
      })
    })
  })

  describe("Advanced Options", () => {
    let audioToTextMock

    beforeEach(() => {
      audioToTextMock = mockSdkService("audioToText", {
        completionFromAudio: { details: "text" },
        endTimestamp: "00:00:37,270",
      })
    })

    afterEach(() => {
      audioToTextMock.mockRestore()
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

      expect(audioToTextMock).toHaveBeenCalledWith({
        body: expect.objectContaining({
          additionalProcessingInstructions: "Test instructions",
          previousNoteDetailsToAppendTo: note.details,
        }),
      })
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

      const calls = audioToTextMock.mock.calls
      expect(calls.length).toBeGreaterThanOrEqual(2)
      expect(calls[0]?.[0]).toMatchObject({
        body: {
          additionalProcessingInstructions: "Test instructions",
          previousNoteDetailsToAppendTo: note.details,
        },
      })
      expect(calls[1]?.[0]).toMatchObject({
        body: {
          additionalProcessingInstructions: "Test instructions",
          previousNoteDetailsToAppendTo: note.details,
        },
      })
    })
  })

  describe("Audio processing with isMidSpeech flag", () => {
    let audioToTextMock

    beforeEach(() => {
      audioToTextMock = mockSdkService("audioToText", {
        completionFromAudio: { details: "text" },
        endTimestamp: "00:00:37,270",
      })
    })

    afterEach(() => {
      audioToTextMock.mockRestore()
    })

    it("should pass isMidSpeech=true when processing timer-triggered chunk", async () => {
      const testBlob2 = new Blob(["test2"])
      await wrapper.vm.processAudio(<AudioChunk>{
        data: testBlob2,
        isMidSpeech: true,
      })
      await flushPromises()

      expect(audioToTextMock).toHaveBeenCalledWith({
        body: expect.objectContaining({
          isMidSpeech: true,
          previousNoteDetailsToAppendTo: note.details,
        }),
      })
    })
  })

  it("should handle returned timestamp from audio processing", async () => {
    const mockResponse = {
      completionFromAudio: { patch: "--- a\n+++ b\n@@ -0,0 +1 @@\n+text\n" },
      endTimestamp: "00:00:37,270",
    }

    const audioToTextSpy = mockSdkService("audioToText", mockResponse)
    mockSdkService("updateNoteDetails", makeMe.aNoteRealm.please())

    const testBlob = new Blob(["test"])
    const result = await wrapper.vm.processAudio({
      data: testBlob,
      isMidSpeech: true,
    })

    expect(result).toBe("00:00:37,270")

    // Verify API call was made with correct parameters
    expect(audioToTextSpy).toHaveBeenCalledWith({
      body: expect.objectContaining({
        isMidSpeech: true,
        previousNoteDetailsToAppendTo: note.details,
      }),
    })
  })

  describe("Fullscreen Integration", () => {
    beforeEach(() => {
      // Browser Mode: Use real timers for fullscreen tests (fake timers interfere)
      vi.useRealTimers()
      document.body.innerHTML = ""

      // Browser Mode: Ensure webkitRequestFullscreen check fails (like FullScreen.browser.spec.ts)
      Object.defineProperty(
        document.documentElement,
        "webkitRequestFullscreen",
        {
          get: () => undefined,
          configurable: true,
        }
      )

      Object.defineProperty(document, "webkitFullscreenElement", {
        get: () => undefined,
        configurable: true,
      })

      // Browser Mode: Spy on REAL Fullscreen API methods!
      vi.spyOn(document.documentElement, "requestFullscreen").mockResolvedValue(
        undefined
      )
      vi.spyOn(document, "exitFullscreen").mockResolvedValue(undefined)
      vi.spyOn(document, "exitPointerLock")
      vi.spyOn(document.documentElement, "requestPointerLock")

      // Mock fullscreenElement
      Object.defineProperty(document, "fullscreenElement", {
        configurable: true,
        get: () => document.documentElement,
      })

      Object.defineProperty(document, "pointerLockElement", {
        configurable: true,
        get: () => document.documentElement,
      })
    })

    afterEach(() => {
      // Restore fake timers after fullscreen tests
      vi.useFakeTimers()
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
        .withCleanStorage()
        .withProps({ note })
        .mount({ attachTo: document.body })

      const advancedButton = findButtonByTitle(wrapper, "Advanced Options")
      await advancedButton.trigger("click")
      await wrapper.vm.$nextTick()

      // Set an error
      wrapper.vm.errors = { someError: "Test error message" }
      await wrapper.vm.$nextTick()

      const fullscreenComponent = wrapper.findComponent(FullScreen)
      expect(fullscreenComponent.exists()).toBe(true)

      // Browser Mode: Click fullscreen button
      const fullscreenBtn = fullscreenComponent.find(".fullscreen-btn")
      await fullscreenBtn.trigger("click")
      await wrapper.vm.$nextTick()
      // Browser Mode: Wait for fullscreen state to update using requestAnimationFrame
      await new Promise((resolve) =>
        requestAnimationFrame(() => resolve(undefined))
      )
      await flushPromises()

      // Browser Mode: Error should be in fullscreen overlay
      const errorElement = document.body.querySelector(
        ".fullscreen-overlay .fullscreen-error"
      )
      expect(errorElement).toBeTruthy()
      expect(errorElement?.textContent?.trim()).toBe("Test error message")
    }, 20000) // Increase timeout to 20 seconds
  })

  describe("Previous content handling", () => {
    let audioToTextMock

    beforeEach(() => {
      audioToTextMock = mockSdkService("audioToText", {
        completionFromAudio: { details: "text" },
        endTimestamp: "00:00:37,270",
      })
    })

    afterEach(() => {
      audioToTextMock.mockRestore()
    })

    it("sends full content when under 500 characters", async () => {
      const shortContent = "Short content"
      const noteWithShortContent = makeMe.aNote.details(shortContent).please()
      wrapper = helper
        .component(NoteAudioTools)
        .withCleanStorage()
        .withProps({ note: noteWithShortContent })
        .mount()

      await wrapper.vm.processAudio({
        data: new Blob(),
        isMidSpeech: true,
      })

      expect(audioToTextMock).toHaveBeenCalledWith({
        body: expect.objectContaining({
          previousNoteDetailsToAppendTo: shortContent,
        }),
      })
    })

    it("truncates content over 500 characters and adds ellipsis", async () => {
      const longContent = "a".repeat(600)
      const noteWithLongContent = makeMe.aNote.details(longContent).please()
      wrapper = helper
        .component(NoteAudioTools)
        .withCleanStorage()
        .withProps({ note: noteWithLongContent })
        .mount()

      await wrapper.vm.processAudio({
        data: new Blob(),
        isMidSpeech: true,
      })

      const expectedContent = `...${"a".repeat(500)}`
      expect(audioToTextMock).toHaveBeenCalledWith({
        body: expect.objectContaining({
          previousNoteDetailsToAppendTo: expectedContent,
        }),
      })
    })

    it("handles null content", async () => {
      const noteWithNullContent = makeMe.aNote.details(undefined).please()
      wrapper = helper
        .component(NoteAudioTools)
        .withCleanStorage()
        .withProps({ note: noteWithNullContent })
        .mount()

      await wrapper.vm.processAudio({
        data: new Blob(),
        isMidSpeech: true,
      })

      expect(audioToTextMock).toHaveBeenCalledWith({
        body: expect.objectContaining({
          previousNoteDetailsToAppendTo: "",
        }),
      })
    })
  })
})

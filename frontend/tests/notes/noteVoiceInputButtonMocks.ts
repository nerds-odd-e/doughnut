import { vi } from "vitest"

export const mockMediaStreamSource = {
  connect: vi.fn(),
  disconnect: vi.fn(),
}

export const mockAudioWorklet = {
  addModule: vi.fn(),
}

export const mockAudioContext = {
  createMediaStreamSource: vi.fn(() => mockMediaStreamSource),
  audioWorklet: mockAudioWorklet,
  destination: {},
}

export const mockAudioWorkletNode = {
  connect: vi.fn(),
  disconnect: vi.fn(),
  port: {
    onmessage: null as ((event: MessageEvent) => void) | null,
    postMessage: vi.fn(),
  },
}

export const mockMediaStop = vi.fn()

export const mockMediaDevices = {
  getUserMedia: vi.fn().mockImplementation(() =>
    Promise.resolve({
      getTracks: () => [{ stop: mockMediaStop }],
      getAudioTracks: () => [
        {
          getSettings: () => ({ deviceId: "device1" }),
        },
      ],
    })
  ),
  addEventListener: vi.fn(),
}

export function clearAudioHardwareMocks() {
  mockMediaStreamSource.connect.mockClear()
  mockMediaStreamSource.disconnect.mockClear()
  mockAudioWorklet.addModule.mockClear()
  mockAudioWorkletNode.connect.mockClear()
  mockAudioWorkletNode.disconnect.mockClear()
  mockAudioWorkletNode.port.postMessage.mockClear()
  mockMediaDevices.getUserMedia.mockClear()
  mockMediaStop.mockClear()
}

export function installAudioBrowserSpies() {
  vi.spyOn(globalThis, "AudioContext").mockImplementation(
    () => mockAudioContext as unknown as AudioContext
  )
  vi.spyOn(globalThis, "AudioWorkletNode").mockImplementation(
    () => mockAudioWorkletNode as unknown as AudioWorkletNode
  )
  Object.defineProperty(globalThis.navigator, "mediaDevices", {
    value: mockMediaDevices,
    writable: true,
    configurable: true,
  })

  const mockContext = {
    clearRect: vi.fn(),
    fillRect: vi.fn(),
    fillStyle: "",
  }
  vi.spyOn(HTMLCanvasElement.prototype, "getContext").mockReturnValue(
    mockContext as unknown as CanvasRenderingContext2D
  )
}

export function recorderWorkletMockExports() {
  return {
    getAudioRecordingWorkerURL: vi.fn(() => "mocked-worker-url"),
  }
}

function mockAudioRecorderMethods(
  onStop?: (
    callback: (chunk: {
      data: File
      isMidSpeech: boolean
    }) => Promise<string | undefined>
  ) => Promise<void>
) {
  return (
    callback: (chunk: {
      data: File
      isMidSpeech: boolean
    }) => Promise<string | undefined>
  ) => ({
    startRecording: vi.fn().mockImplementation(async () => {
      mockMediaDevices.getUserMedia({ audio: true })
      mockMediaStreamSource.connect(mockAudioWorkletNode)
      mockAudioWorkletNode.connect(mockAudioContext.destination)
    }),
    stopRecording: vi.fn().mockImplementation(async () => {
      if (onStop) {
        return onStop(callback)
      }
      mockAudioWorkletNode.disconnect()
      mockMediaStreamSource.disconnect()
      mockMediaStop()
    }),
    getAudioData: vi.fn(() => 0),
    hasUnconvertedAudio: vi.fn(() => false),
  })
}

export function audioRecorderMockExports() {
  return {
    createAudioRecorder: vi.fn(mockAudioRecorderMethods()),
  }
}

/** Invokes the processor callback at Stop (for title speech / convert-only-at-stop). */
export function audioRecorderInvokingCallbackMockExports() {
  return {
    createAudioRecorder: vi.fn(
      mockAudioRecorderMethods(async (callback) => {
        await callback({
          data: new File([], "test.webm"),
          isMidSpeech: false,
        })
        mockAudioWorkletNode.disconnect()
        mockMediaStreamSource.disconnect()
        mockMediaStop()
      })
    ),
  }
}

export function wakeLockerMockExports() {
  return {
    createWakeLocker: vi.fn(() => ({
      request: vi.fn().mockResolvedValue(undefined),
      release: vi.fn().mockResolvedValue(undefined),
    })),
  }
}

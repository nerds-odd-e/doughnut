import { ref, type Ref } from "vue"
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

export const mockDevices = [
  { deviceId: "device1", kind: "audioinput", label: "Microphone 1" },
  { deviceId: "device2", kind: "audioinput", label: "Microphone 2" },
]

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
  enumerateDevices: vi
    .fn()
    .mockImplementation(() => Promise.resolve(mockDevices)),
  addEventListener: vi.fn(),
}

export const mockCreateObjectURL = vi.fn(
  (blob: Blob | MediaSource) =>
    `blob:${(blob as Blob).type || "unknown"}-mocked-url`
)
export const mockRevokeObjectURL = vi.fn()

export function clearAudioHardwareMocks() {
  mockMediaStreamSource.connect.mockClear()
  mockMediaStreamSource.disconnect.mockClear()
  mockAudioWorklet.addModule.mockClear()
  mockAudioWorkletNode.connect.mockClear()
  mockAudioWorkletNode.disconnect.mockClear()
  mockAudioWorkletNode.port.postMessage.mockClear()
  mockMediaDevices.getUserMedia.mockClear()
  mockMediaDevices.enumerateDevices.mockClear()
  mockMediaStop.mockClear()
  mockCreateObjectURL.mockClear()
  mockRevokeObjectURL.mockClear()
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
  vi.spyOn(URL, "createObjectURL").mockImplementation(mockCreateObjectURL)
  vi.spyOn(URL, "revokeObjectURL").mockImplementation(mockRevokeObjectURL)

  const mockContext = {
    drawImage: vi.fn(),
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

export function audioRecorderMockExports() {
  return {
    createAudioRecorder: vi.fn(() => ({
      startRecording: vi.fn().mockImplementation(async () => {
        mockMediaDevices.getUserMedia({ audio: true })
        mockMediaStreamSource.connect(mockAudioWorkletNode)
        mockAudioWorkletNode.connect(mockAudioContext.destination)
      }),
      stopRecording: vi.fn().mockImplementation(async () => {
        mockAudioWorkletNode.disconnect()
        mockMediaStreamSource.disconnect()
        mockMediaStop()
        return new File([], "test.webm")
      }),
      getAudioData: vi.fn(() => 0),
      tryFlush: vi.fn().mockResolvedValue(undefined),
      getAudioDevices: vi.fn().mockImplementation(() => {
        mockMediaDevices.enumerateDevices()
        return ref(mockDevices) as Ref<MediaDeviceInfo[]>
      }),
      getSelectedDevice: vi.fn(() => ref("device1")),
      switchAudioDevice: vi
        .fn()
        .mockImplementation(async (deviceId: string) => {
          mockMediaDevices.getUserMedia({
            audio: { deviceId: { exact: deviceId } },
          })
        }),
    })),
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

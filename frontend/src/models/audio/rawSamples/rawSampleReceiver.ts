import { createAudioBuffer, type AudioBuffer } from "./rawSampleAudioBuffer"
import { getAudioRecordingWorkerURL } from "./recorderWorklet"

const SAMPLE_RATE = 16000

export interface AudioReceiver {
  initialize: (deviceId?: string) => Promise<void>
  disconnect: () => void
  isInitialized: () => boolean
  reconnect: (deviceId: string) => Promise<void>
  getBuffer: () => AudioBuffer
}

export class RawAudioReceiver implements AudioReceiver {
  private audioContext: AudioContext | null = null
  private mediaStream: MediaStream | null = null
  private audioInput: MediaStreamAudioSourceNode | null = null
  private workletNode: AudioWorkletNode | null = null
  private audioBuffer: AudioBuffer

  constructor() {
    this.audioBuffer = createAudioBuffer(SAMPLE_RATE)
  }

  async initialize(deviceId?: string): Promise<void> {
    try {
      this.mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: deviceId ? { deviceId: { exact: deviceId } } : true,
      })

      const audioWorkletUrl = getAudioRecordingWorkerURL()
      this.audioContext = new AudioContext({ sampleRate: SAMPLE_RATE })
      await this.audioContext.audioWorklet.addModule(audioWorkletUrl)
      this.audioInput = this.audioContext.createMediaStreamSource(
        this.mediaStream
      )
      this.workletNode = new AudioWorkletNode(
        this.audioContext,
        "recorder-worklet-processor"
      )

      this.workletNode.port.onmessage = (event) => {
        if (event.data.audioBuffer) {
          this.audioBuffer.receiveAudioData(event.data.audioBuffer)
        }
      }
      this.audioInput.connect(this.workletNode)
      this.workletNode.connect(this.audioContext.destination)
    } catch (error) {
      console.error("Error initializing audio receiver:", error)
      throw new Error("Failed to initialize audio receiver")
    }
  }

  disconnect(): void {
    if (this.workletNode) {
      this.workletNode.disconnect()
    }
    if (this.audioInput) {
      this.audioInput.disconnect()
    }
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach((track) => track.stop())
    }
  }

  isInitialized(): boolean {
    return !!this.audioContext
  }

  async reconnect(deviceId: string): Promise<void> {
    this.disconnect()
    await this.initialize(deviceId)
  }

  getBuffer(): AudioBuffer {
    return this.audioBuffer
  }
}

export function createRawSampleAudioReceiver(): AudioReceiver {
  return new RawAudioReceiver()
}

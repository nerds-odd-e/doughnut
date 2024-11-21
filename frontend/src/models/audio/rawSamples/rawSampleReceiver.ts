import { createAudioBuffer } from "./rawSampleAudioBuffer"
import { getAudioRecordingWorkerURL } from "./recorderWorklet"
import type { AudioReceiver, AudioBuffer } from "../audioReceiver"

const SAMPLE_RATE = 16000

export class RawAudioReceiver implements AudioReceiver {
  private audioContext: AudioContext | null = null
  private audioInput: MediaStreamAudioSourceNode | null = null
  private workletNode: AudioWorkletNode | null = null
  private audioBuffer = createAudioBuffer(SAMPLE_RATE)

  async connect(mediaStream: MediaStream): Promise<void> {
    try {
      const audioWorkletUrl = getAudioRecordingWorkerURL()
      this.audioContext = new AudioContext({ sampleRate: SAMPLE_RATE })
      await this.audioContext.audioWorklet.addModule(audioWorkletUrl)
      this.audioInput = this.audioContext.createMediaStreamSource(mediaStream)
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
      console.error("Error connecting audio receiver:", error)
      throw new Error("Failed to connect audio receiver")
    }
  }

  disconnect(): void {
    if (this.workletNode) {
      this.workletNode.disconnect()
    }
    if (this.audioInput) {
      this.audioInput.disconnect()
    }
  }

  isInitialized(): boolean {
    return !!this.audioContext
  }

  async reconnect(deviceId: string): Promise<void> {
    this.disconnect()
    await this.connect(
      await navigator.mediaDevices.getUserMedia({
        audio: deviceId ? { deviceId: { exact: deviceId } } : true,
      })
    )
  }

  getBuffer(): AudioBuffer {
    return this.audioBuffer
  }

  getCurrentAverageSample(): number {
    return this.audioBuffer.getCurrentAverageSample()
  }
}

export function createRawSampleAudioReceiver(): AudioReceiver {
  return new RawAudioReceiver()
}

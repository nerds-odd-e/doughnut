import { createAudioBuffer, type AudioBuffer } from "./audioBuffer"
import { getAudioRecordingWorkerURL } from "./recorderWorklet"

const SAMPLE_RATE = 16000

export interface AudioReceiver {
  initialize: (deviceId?: string) => Promise<void>
  disconnect: () => void
  isInitialized: () => boolean
  reconnect: (deviceId: string) => Promise<void>
  getBuffer: () => AudioBuffer
}

export const createAudioReceiver = (): AudioReceiver => {
  let audioContext: AudioContext | null = null
  let mediaStream: MediaStream | null = null
  let audioInput: MediaStreamAudioSourceNode | null = null
  let workletNode: AudioWorkletNode | null = null
  const audioBuffer = createAudioBuffer(SAMPLE_RATE)

  return {
    initialize: async (deviceId?: string): Promise<void> => {
      try {
        mediaStream = await navigator.mediaDevices.getUserMedia({
          audio: deviceId ? { deviceId: { exact: deviceId } } : true,
        })

        const audioWorkletUrl = getAudioRecordingWorkerURL()
        audioContext = new AudioContext({ sampleRate: SAMPLE_RATE })
        await audioContext.audioWorklet.addModule(audioWorkletUrl)
        audioInput = audioContext.createMediaStreamSource(mediaStream)
        workletNode = new AudioWorkletNode(
          audioContext,
          "recorder-worklet-processor"
        )

        workletNode.port.onmessage = (event) => {
          if (event.data.audioBuffer) {
            audioBuffer.receiveAudioData(event.data.audioBuffer)
          }
        }
        audioInput.connect(workletNode)
        workletNode.connect(audioContext.destination)
      } catch (error) {
        console.error("Error initializing audio receiver:", error)
        throw new Error("Failed to initialize audio receiver")
      }
    },

    disconnect: () => {
      if (workletNode) {
        workletNode.disconnect()
      }
      if (audioInput) {
        audioInput.disconnect()
      }
      if (mediaStream) {
        mediaStream.getTracks().forEach((track) => track.stop())
      }
    },

    isInitialized: () => !!audioContext,

    async reconnect(deviceId: string): Promise<void> {
      this.disconnect()
      await this.initialize(deviceId)
    },

    getBuffer: () => audioBuffer,
  }
}

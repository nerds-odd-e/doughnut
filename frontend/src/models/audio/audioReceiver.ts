import { getAudioRecordingWorkerURL } from "./recorderWorklet"

export interface AudioReceiver {
  initialize: (deviceId?: string) => Promise<void>
  disconnect: () => void
  isInitialized: () => boolean
  reconnect: (deviceId: string) => Promise<void>
}

export const createAudioReceiver = (
  onAudioData: (audioBuffer: Float32Array[]) => void
): AudioReceiver => {
  let audioContext: AudioContext | null = null
  let mediaStream: MediaStream | null = null
  let audioInput: MediaStreamAudioSourceNode | null = null
  let workletNode: AudioWorkletNode | null = null

  const disconnect = () => {
    if (workletNode) {
      workletNode.disconnect()
    }
    if (audioInput) {
      audioInput.disconnect()
    }
    if (mediaStream) {
      mediaStream.getTracks().forEach((track) => track.stop())
    }
  }

  return {
    initialize: async (deviceId?: string): Promise<void> => {
      try {
        mediaStream = await navigator.mediaDevices.getUserMedia({
          audio: deviceId ? { deviceId: { exact: deviceId } } : true,
        })

        const audioWorkletUrl = getAudioRecordingWorkerURL()
        audioContext = new AudioContext({ sampleRate: 16000 })
        await audioContext.audioWorklet.addModule(audioWorkletUrl)
        audioInput = audioContext.createMediaStreamSource(mediaStream)
        workletNode = new AudioWorkletNode(
          audioContext,
          "recorder-worklet-processor"
        )

        workletNode.port.onmessage = (event) => {
          if (event.data.audioBuffer) {
            onAudioData(event.data.audioBuffer)
          }
        }
        audioInput.connect(workletNode)
        workletNode.connect(audioContext.destination)
      } catch (error) {
        console.error("Error initializing audio receiver:", error)
        throw new Error("Failed to initialize audio receiver")
      }
    },

    disconnect,

    isInitialized: () => !!audioContext,

    async reconnect(deviceId: string): Promise<void> {
      disconnect()
      await this.initialize(deviceId)
    },
  }
}

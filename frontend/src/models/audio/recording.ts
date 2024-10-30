import { getAudioRecordingWorkerURL } from "./recorderWorklet"
import { type AudioProcessor, createAudioProcessor } from "./audioProcessor"

export interface AudioRecorder {
  startRecording: () => Promise<void>
  stopRecording: () => Promise<File>
  getAudioData: () => Float32Array[]
  flush: () => Promise<void>
}

export const createAudioRecorder = (
  processorCallback: (file: File) => Promise<void>
): AudioRecorder => {
  let audioContext: AudioContext | null = null
  let mediaStream: MediaStream | null = null
  let audioInput: MediaStreamAudioSourceNode | null = null
  let workletNode: AudioWorkletNode | null = null
  const audioProcessor: AudioProcessor = createAudioProcessor(
    16000,
    processorCallback
  )

  const audioRecorder: AudioRecorder = {
    startRecording: async function (): Promise<void> {
      const audioWorkletUrl = getAudioRecordingWorkerURL()
      try {
        audioContext = new AudioContext({ sampleRate: 16000 })

        await audioContext.audioWorklet.addModule(audioWorkletUrl)

        mediaStream = await navigator.mediaDevices.getUserMedia({
          audio: {
            autoGainControl: true,
            echoCancellation: true,
            noiseSuppression: true,
            deviceId: "default",
          },
        })
        audioInput = audioContext.createMediaStreamSource(mediaStream)

        workletNode = new AudioWorkletNode(
          audioContext,
          "recorder-worklet-processor"
        )

        workletNode.port.onmessage = (event) => {
          if (event.data.audioBuffer) {
            audioProcessor.processAudioData(event.data.audioBuffer)
          }
        }

        audioProcessor.start()
        audioInput.connect(workletNode)
        workletNode.connect(audioContext.destination)
      } catch (error) {
        console.error("Error starting recording:", error)
        throw new Error("Failed to start recording")
      }
    },

    stopRecording: async function (): Promise<File> {
      if (workletNode) {
        workletNode.disconnect()
      }
      if (audioInput) {
        audioInput.disconnect()
      }
      if (mediaStream) {
        mediaStream.getTracks().forEach((track) => track.stop())
      }

      return audioProcessor.stop()
    },

    getAudioData: function (): Float32Array[] {
      return audioProcessor.getAudioData()
    },

    flush: async function (): Promise<void> {
      await audioProcessor.flush()
    },
  }

  return audioRecorder
}

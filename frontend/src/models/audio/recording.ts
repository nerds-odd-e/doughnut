import { type Ref, ref } from "vue"
import { getAudioRecordingWorkerURL } from "./recorderWorklet"
import {
  type AudioChunk,
  type AudioProcessor,
  createAudioProcessor,
} from "./audioProcessor"

export interface AudioRecorder {
  startRecording: () => Promise<void>
  stopRecording: () => Promise<File>
  getAudioData: () => Float32Array[]
  tryFlush: () => Promise<void>
  getAudioDevices: () => Ref<MediaDeviceInfo[]>
  getSelectedDevice: () => Ref<string>
  switchAudioDevice: (deviceId: string) => Promise<void>
}

export const createAudioRecorder = (
  processorCallback: (chunk: AudioChunk) => Promise<string | undefined>
): AudioRecorder => {
  let audioContext: AudioContext | null = null
  let mediaStream: MediaStream | null = null
  let audioInput: MediaStreamAudioSourceNode | null = null
  let workletNode: AudioWorkletNode | null = null
  const audioProcessor: AudioProcessor = createAudioProcessor(
    16000,
    processorCallback
  )
  let isRecording: boolean = false
  const audioDevices: Ref<MediaDeviceInfo[]> = ref([])
  const selectedDevice: Ref<string> = ref("")

  const audioRecorder: AudioRecorder = {
    startRecording: async function (): Promise<void> {
      const audioWorkletUrl = getAudioRecordingWorkerURL()
      try {
        audioContext = new AudioContext({ sampleRate: 16000 })

        await audioContext.audioWorklet.addModule(audioWorkletUrl)

        mediaStream = await navigator.mediaDevices.getUserMedia({
          audio: true,
        })
        const devices = await navigator.mediaDevices.enumerateDevices()
        audioDevices.value = devices.filter(
          (device) => device.kind === "audioinput"
        )

        const currentTrack = mediaStream.getAudioTracks()[0]
        const currentDeviceId = currentTrack?.getSettings().deviceId
        selectedDevice.value = currentDeviceId || ""

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
        isRecording = true
      } catch (error) {
        console.error("Error starting recording:", error)
        throw new Error("Failed to start recording")
      }
    },

    stopRecording: async function (): Promise<File> {
      isRecording = false
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

    tryFlush: async function (): Promise<void> {
      await audioProcessor.tryFlush()
    },

    getAudioDevices: function (): Ref<MediaDeviceInfo[]> {
      return audioDevices
    },

    getSelectedDevice: function (): Ref<string> {
      return selectedDevice
    },

    switchAudioDevice: async function (deviceId: string): Promise<void> {
      if (selectedDevice.value === deviceId) return

      selectedDevice.value = deviceId
      if (isRecording) {
        // Stop current recording
        if (workletNode) workletNode.disconnect()
        if (audioInput) audioInput.disconnect()
        if (mediaStream)
          mediaStream.getTracks().forEach((track) => track.stop())

        // Restart with new device
        mediaStream = await navigator.mediaDevices.getUserMedia({
          audio: { deviceId: { exact: deviceId } },
        })
        audioInput = audioContext!.createMediaStreamSource(mediaStream)
        audioInput.connect(workletNode!)
        workletNode!.connect(audioContext!.destination)
      }
    },
  }

  // Add device change listener
  navigator.mediaDevices.addEventListener("devicechange", async () => {
    const devices = await navigator.mediaDevices.enumerateDevices()
    audioDevices.value = devices.filter(
      (device) => device.kind === "audioinput"
    )

    // Check if selected device still exists
    const deviceExists = audioDevices.value.some(
      (device) => device.deviceId === selectedDevice.value
    )
    if (!deviceExists && audioDevices.value.length > 0) {
      // Switch to first available device if current one is disconnected
      await audioRecorder.switchAudioDevice(audioDevices.value[0]?.deviceId!)
    }
  })

  return audioRecorder
}

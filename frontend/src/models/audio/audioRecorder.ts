import { type Ref, ref } from "vue"
import {
  type AudioChunk,
  wireAudioProcessingScheduler,
} from "./audioProcessingScheduler"
import { createAudioReceiver } from "./audioReceiver"

export interface AudioRecorder {
  startRecording: () => Promise<void>
  stopRecording: () => Promise<File>
  getAudioData: () => number
  tryFlush: () => Promise<void>
  getAudioDevices: () => Ref<MediaDeviceInfo[]>
  getSelectedDevice: () => Ref<string>
  switchAudioDevice: (deviceId: string) => Promise<void>
}

export const createAudioRecorder = (
  processorCallback: (chunk: AudioChunk) => Promise<string | undefined>
): AudioRecorder => {
  const audioReceiver = createAudioReceiver()
  const audioProcessingScheduler = wireAudioProcessingScheduler(
    audioReceiver.getBuffer(),
    processorCallback
  )
  let isRecording: boolean = false
  const audioDevices: Ref<MediaDeviceInfo[]> = ref([])
  const selectedDevice: Ref<string> = ref("")
  let mediaStream: MediaStream | null = null

  const audioRecorder: AudioRecorder = {
    startRecording: async function (): Promise<void> {
      try {
        const devices = await navigator.mediaDevices.enumerateDevices()
        audioDevices.value = devices.filter(
          (device) => device.kind === "audioinput"
        )

        mediaStream = await navigator.mediaDevices.getUserMedia({
          audio: true,
        })
        const currentTrack = mediaStream.getAudioTracks()[0]
        const currentDeviceId = currentTrack?.getSettings().deviceId
        selectedDevice.value = currentDeviceId || ""

        await audioReceiver.connect(mediaStream)
        audioProcessingScheduler.start()
        isRecording = true
      } catch (error) {
        console.error("Error starting recording:", error)
        throw new Error("Failed to start recording")
      }
    },

    stopRecording: async function (): Promise<File> {
      isRecording = false
      audioReceiver.disconnect()
      if (mediaStream) {
        mediaStream.getTracks().forEach((track) => track.stop())
        mediaStream = null
      }
      return audioProcessingScheduler.stop()
    },

    getAudioData: function (): number {
      return audioReceiver.getCurrentAverageSample()
    },

    tryFlush: async function (): Promise<void> {
      await audioProcessingScheduler.tryFlush()
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
        audioReceiver.disconnect()
        if (mediaStream) {
          mediaStream.getTracks().forEach((track) => track.stop())
        }
        mediaStream = await navigator.mediaDevices.getUserMedia({
          audio: { deviceId: { exact: deviceId } },
        })
        await audioReceiver.connect(mediaStream)
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

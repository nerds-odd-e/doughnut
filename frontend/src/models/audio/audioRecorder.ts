import {
  type AudioChunk,
  type AudioProcessingSchedulerOptions,
  wireAudioProcessingScheduler,
} from "./audioProcessingScheduler"
import { createAudioReceiver } from "./audioReceiver"

export interface AudioRecorder {
  startRecording: () => Promise<void>
  stopRecording: () => Promise<void>
  getAudioData: () => number
  hasUnconvertedAudio: () => boolean
}

export type AudioRecorderOptions = AudioProcessingSchedulerOptions

export const createAudioRecorder = (
  processorCallback: (chunk: AudioChunk) => Promise<string | undefined>,
  options?: AudioRecorderOptions
): AudioRecorder => {
  const audioReceiver = createAudioReceiver()
  const audioProcessingScheduler = wireAudioProcessingScheduler(
    audioReceiver.getBuffer(),
    processorCallback,
    options
  )
  let isRecording: boolean = false
  let selectedDevice = ""
  let mediaStream: MediaStream | null = null

  const switchAudioDevice = async (deviceId: string): Promise<void> => {
    selectedDevice = deviceId
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
  }

  const switchWhenSelectedDeviceDisconnects = async () => {
    const devices = await navigator.mediaDevices.enumerateDevices()
    const audioDevices = devices.filter(
      (device) => device.kind === "audioinput"
    )

    const deviceExists = audioDevices.some(
      (device) => device.deviceId === selectedDevice
    )
    if (!deviceExists && audioDevices.length > 0) {
      await switchAudioDevice(audioDevices[0]?.deviceId!)
    }
  }

  const audioRecorder: AudioRecorder = {
    startRecording: async function (): Promise<void> {
      mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: true,
      })
      const currentTrack = mediaStream.getAudioTracks()[0]
      selectedDevice = currentTrack?.getSettings().deviceId || ""

      await audioReceiver.connect(mediaStream)
      audioProcessingScheduler.start()
      isRecording = true
      navigator.mediaDevices.addEventListener(
        "devicechange",
        switchWhenSelectedDeviceDisconnects
      )
    },

    stopRecording: async function (): Promise<void> {
      isRecording = false
      navigator.mediaDevices.removeEventListener(
        "devicechange",
        switchWhenSelectedDeviceDisconnects
      )
      audioReceiver.disconnect()
      if (mediaStream) {
        mediaStream.getTracks().forEach((track) => track.stop())
        mediaStream = null
      }
      await audioProcessingScheduler.stop()
    },

    getAudioData: function (): number {
      return audioReceiver.getCurrentAverageSample()
    },

    hasUnconvertedAudio: function (): boolean {
      return audioReceiver.getBuffer().hasUnprocessedData()
    },
  }

  return audioRecorder
}

export interface AudioRecorder {
  startRecording: () => Promise<void>
  stopRecording: () => File
}

export const createAudioRecorder = (): AudioRecorder => {
  let audioContext: AudioContext | null = null
  let mediaStream: MediaStream | null = null
  let audioInput: MediaStreamAudioSourceNode | null = null
  let recorder: ScriptProcessorNode | null = null
  let audioData: Float32Array[] = []

  const audioRecorder: AudioRecorder = {
    startRecording: async function (): Promise<void> {
      try {
        audioContext = new AudioContext()
        mediaStream = await navigator.mediaDevices.getUserMedia({
          audio: true,
        })
        audioInput = audioContext.createMediaStreamSource(mediaStream)

        const bufferSize = 4096
        recorder = audioContext.createScriptProcessor(bufferSize, 1, 1)

        recorder.onaudioprocess = (event) => {
          const channelData = event.inputBuffer.getChannelData(0)
          audioData.push(new Float32Array(channelData))
        }

        audioInput.connect(recorder)
        recorder.connect(audioContext.destination)
      } catch (error) {
        console.error("Error starting recording:", error)
        throw new Error("Failed to start recording")
      }
    },

    stopRecording: function (): File {
      if (recorder) {
        recorder.disconnect()
      }
      if (audioInput) {
        audioInput.disconnect()
      }
      if (mediaStream) {
        mediaStream.getTracks().forEach((track) => track.stop())
      }

      const wavBlob = encodeWAV(audioData, audioContext?.sampleRate || 44100)
      const fileName = `recorded_audio_${new Date().toISOString()}.wav`
      const file = new File([wavBlob], fileName, { type: "audio/wav" })

      // Reset the audioData
      audioData = []
      return file
    },
  }

  return audioRecorder
}

const encodeWAV = (samples: Float32Array[], sampleRate: number): Blob => {
  const bufferLength = samples.reduce((acc, sample) => acc + sample.length, 0)
  const buffer = new ArrayBuffer(44 + bufferLength * 2)
  const view = new DataView(buffer)

  writeString(view, 0, "RIFF")
  view.setUint32(4, 36 + bufferLength * 2, true)
  writeString(view, 8, "WAVE")
  writeString(view, 12, "fmt ")
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, 1, true)
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * 2, true)
  view.setUint16(32, 2, true)
  view.setUint16(34, 16, true)
  writeString(view, 36, "data")
  view.setUint32(40, bufferLength * 2, true)

  let offset = 44
  samples.forEach((sample) => {
    for (let i = 0; i < sample.length; i++, offset += 2) {
      const s = Math.max(-1, Math.min(1, sample[i] ?? 0))
      view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7fff, true)
    }
  })

  return new Blob([view], { type: "audio/wav" })
}

const writeString = (view: DataView, offset: number, string: string): void => {
  for (let i = 0; i < string.length; i++) {
    view.setUint8(offset + i, string.charCodeAt(i))
  }
}

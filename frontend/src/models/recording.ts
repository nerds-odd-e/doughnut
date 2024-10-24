export interface AudioRecorder {
  audioContext: AudioContext | null
  mediaStream: MediaStream | null
  audioInput: MediaStreamAudioSourceNode | null
  recorder: ScriptProcessorNode | null
  audioData: Float32Array[]
}

export const createAudioRecorder = (): AudioRecorder => ({
  audioContext: null,
  mediaStream: null,
  audioInput: null,
  recorder: null,
  audioData: [],
})

export const startRecording = async (
  audioRecorder: AudioRecorder
): Promise<void> => {
  try {
    audioRecorder.audioContext = new AudioContext()
    audioRecorder.mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: true,
    })
    audioRecorder.audioInput =
      audioRecorder.audioContext.createMediaStreamSource(
        audioRecorder.mediaStream
      )

    const bufferSize = 4096
    audioRecorder.recorder = audioRecorder.audioContext.createScriptProcessor(
      bufferSize,
      1,
      1
    )

    audioRecorder.recorder.onaudioprocess = (event) => {
      const channelData = event.inputBuffer.getChannelData(0)
      audioRecorder.audioData.push(new Float32Array(channelData))
    }

    audioRecorder.audioInput.connect(audioRecorder.recorder)
    audioRecorder.recorder.connect(audioRecorder.audioContext.destination)
  } catch (error) {
    console.error("Error starting recording:", error)
    throw new Error("Failed to start recording")
  }
}

export const stopRecording = (audioRecorder: AudioRecorder): File => {
  if (audioRecorder.recorder) {
    audioRecorder.recorder.disconnect()
  }
  if (audioRecorder.audioInput) {
    audioRecorder.audioInput.disconnect()
  }
  if (audioRecorder.mediaStream) {
    audioRecorder.mediaStream.getTracks().forEach((track) => track.stop())
  }

  const wavBlob = encodeWAV(
    audioRecorder.audioData,
    audioRecorder.audioContext?.sampleRate || 44100
  )
  const fileName = `recorded_audio_${new Date().toISOString()}.wav`
  const file = new File([wavBlob], fileName, { type: "audio/wav" })

  audioRecorder.audioData = []
  return file
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

export interface AudioProcessor {
  processAudioData: (newData: Float32Array[]) => void
  getAudioData: () => Float32Array[]
  start: () => void
  stop: () => Promise<File>
  flush: () => Promise<void>
}

export interface AudioChunk {
  data: File
  incomplete: boolean
}

export const createAudioProcessor = (
  sampleRate: number,
  processorCallback: (chunk: AudioChunk) => Promise<void>
): AudioProcessor => {
  const audioData: Float32Array[] = []
  let lastProcessedIndex = 0
  let processorTimer: NodeJS.Timeout | null = null
  let silenceCounter = 0
  const SILENCE_THRESHOLD = 0.01
  const SILENCE_DURATION_THRESHOLD = 3 * sampleRate // 2 seconds of silence

  const isSilent = (data: Float32Array): boolean => {
    let sum = 0
    for (let i = 0; i < data.length; i++) {
      sum += Math.abs(data[i] ?? 0)
    }
    const avg = sum / data.length
    return avg < SILENCE_THRESHOLD
  }

  const processAndCallback = async (isIncomplete: boolean = true) => {
    if (audioData.length > lastProcessedIndex) {
      const dataToProcess = audioData.slice(lastProcessedIndex)
      lastProcessedIndex = audioData.length
      const isAllSilent = dataToProcess.every((chunk) => isSilent(chunk))
      if (!isAllSilent) {
        const file = createAudioFile(dataToProcess, sampleRate, true)
        await processorCallback({
          data: file,
          incomplete: isIncomplete,
        })
      }
    }
  }

  const startTimer = () => {
    processorTimer = setInterval(() => {
      processAndCallback()
    }, 60 * 1000)
  }

  return {
    processAudioData(newData: Float32Array[]) {
      newData.forEach((chunk) => {
        if (isSilent(chunk)) {
          silenceCounter += chunk.length
          if (silenceCounter >= SILENCE_DURATION_THRESHOLD) {
            this.flush()
            silenceCounter = 0
          }
        } else {
          silenceCounter = 0
        }

        // Add the chunk to audioData (silent or not)
        audioData.push(chunk)
      })
    },

    start: () => {
      startTimer()
    },

    async stop() {
      if (processorTimer) {
        clearInterval(processorTimer)
        processorTimer = null
      }
      await this.flush()
      return createAudioFile(audioData, sampleRate, false)
    },

    getAudioData: () => {
      return audioData
    },

    flush: async (): Promise<void> => {
      if (processorTimer) {
        clearInterval(processorTimer)
        startTimer()
      }
      await processAndCallback(false)
    },
  }
}

// Helper function to create audio files
const createAudioFile = (
  data: Float32Array[],
  sampleRate: number,
  isPartial: boolean
): File => {
  const wavBlob = encodeWAV(data, sampleRate)
  const timestamp = new Date().toISOString()
  const fileName = `recorded_audio_${isPartial ? "partial_" : ""}${timestamp}.wav`
  return new File([wavBlob], fileName, { type: "audio/wav" })
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

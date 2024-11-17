export interface AudioProcessor {
  processAudioData(newData: Float32Array[]): void
  getAudioData(): Float32Array[]
  start(): void
  stop(): Promise<File>
  tryFlush(): Promise<void>
}

export interface AudioChunk {
  data: File
  incomplete: boolean
}

class AudioProcessorImpl implements AudioProcessor {
  private audioData: Float32Array[] = []
  private lastProcessedArrayIndex = 0
  private lastProcessedInternalIndex = 0
  private processorTimer: NodeJS.Timeout | null = null
  private silenceCounter = 0
  private readonly SILENCE_THRESHOLD = 0.01
  private readonly SILENCE_DURATION_THRESHOLD: number
  private isProcessing = false

  constructor(
    private readonly sampleRate: number,
    private readonly processorCallback: (
      chunk: AudioChunk
    ) => Promise<string | undefined>
  ) {
    this.SILENCE_DURATION_THRESHOLD = 3 * this.sampleRate
  }

  private isSilent(data: Float32Array): boolean {
    const sum = data.reduce((acc, val) => acc + Math.abs(val), 0)
    const avg = sum / data.length
    return avg < this.SILENCE_THRESHOLD
  }

  private async processDataChunk(isIncomplete = true): Promise<void> {
    if (this.audioData.length <= this.lastProcessedArrayIndex) return

    const dataToProcess: Float32Array[] = []

    // Slice the first chunk from the last processed internal index
    const firstChunk = this.audioData[this.lastProcessedArrayIndex]
    if (firstChunk) {
      dataToProcess.push(firstChunk.slice(this.lastProcessedInternalIndex))
    }

    // Add the remaining chunks
    dataToProcess.push(
      ...this.audioData.slice(this.lastProcessedArrayIndex + 1)
    )

    if (dataToProcess.length === 0) return

    const isAllSilent = dataToProcess.every((chunk) => this.isSilent(chunk))
    if (isAllSilent) return

    // Create an audio file from the data chunks
    const file = createAudioFile(dataToProcess, this.sampleRate, isIncomplete)

    // Store current indices before processing
    const newLastProcessedArrayIndex = this.audioData.length
    const newLastProcessedInternalIndex = 0

    const timestamp = await this.processorCallback({
      data: file,
      incomplete: isIncomplete,
    })

    if (timestamp && typeof timestamp === "string") {
      const processedSeconds = parseTimestamp(timestamp)
      if (processedSeconds !== undefined) {
        const processedSamples = Math.floor(processedSeconds * this.sampleRate)
        let totalSamples = this.lastProcessedInternalIndex

        for (
          let i = this.lastProcessedArrayIndex;
          i < this.audioData.length;
          i++
        ) {
          const arrayLength = this.audioData[i]?.length ?? 0
          if (totalSamples + arrayLength <= processedSamples) {
            totalSamples += arrayLength
            this.lastProcessedArrayIndex = i + 1
            this.lastProcessedInternalIndex = 0
          } else {
            this.lastProcessedArrayIndex = i
            this.lastProcessedInternalIndex = processedSamples - totalSamples
            break
          }
        }
      } else {
        // Use new indices if timestamp parsing fails
        this.lastProcessedArrayIndex = newLastProcessedArrayIndex
        this.lastProcessedInternalIndex = newLastProcessedInternalIndex
      }
    } else {
      // Use new indices if no timestamp is provided
      this.lastProcessedArrayIndex = newLastProcessedArrayIndex
      this.lastProcessedInternalIndex = newLastProcessedInternalIndex
    }
  }

  private async processAndCallback(isIncomplete = true): Promise<void> {
    if (this.isProcessing) return

    this.isProcessing = true
    try {
      await this.processDataChunk(isIncomplete)
    } finally {
      this.isProcessing = false
    }
  }

  private startTimer(): void {
    this.processorTimer = setInterval(() => {
      this.processAndCallback()
    }, 20 * 1000)
  }

  processAudioData(newData: Float32Array[]): void {
    for (const chunk of newData) {
      if (this.isSilent(chunk)) {
        this.silenceCounter += chunk.length
        if (this.silenceCounter >= this.SILENCE_DURATION_THRESHOLD) {
          this.tryFlush()
          this.silenceCounter = 0
        }
      } else {
        this.silenceCounter = 0
      }
      this.audioData.push(chunk)
    }
  }

  start(): void {
    this.startTimer()
  }

  async stop(): Promise<File> {
    if (this.processorTimer) {
      clearInterval(this.processorTimer)
      this.processorTimer = null
    }

    // Wait for any ongoing processing to complete
    while (this.isProcessing) {
      await new Promise((resolve) => setTimeout(resolve, 10))
    }

    // Process any remaining data
    const hasUnprocessedData =
      this.lastProcessedArrayIndex < this.audioData.length ||
      (this.lastProcessedArrayIndex === this.audioData.length - 1 &&
        this.lastProcessedInternalIndex <
          (this.audioData[this.lastProcessedArrayIndex]?.length ?? 0))

    if (hasUnprocessedData) {
      this.isProcessing = true
      try {
        await this.processDataChunk(false)
      } finally {
        this.isProcessing = false
      }
    }

    return createAudioFile(this.audioData, this.sampleRate, false)
  }

  getAudioData(): Float32Array[] {
    return this.audioData
  }

  async tryFlush(): Promise<void> {
    if (this.processorTimer) {
      clearInterval(this.processorTimer)
      this.startTimer()
    }
    await this.processAndCallback(false)
  }
}

export const createAudioProcessor = (
  sampleRate: number,
  processorCallback: (chunk: AudioChunk) => Promise<string | undefined>
): AudioProcessor => {
  return new AudioProcessorImpl(sampleRate, processorCallback)
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

  // Write WAV header
  writeString(view, 0, "RIFF")
  view.setUint32(4, 36 + bufferLength * 2, true)
  writeString(view, 8, "WAVE")
  writeString(view, 12, "fmt ")
  view.setUint32(16, 16, true) // Subchunk1Size (16 for PCM)
  view.setUint16(20, 1, true) // AudioFormat (1 for PCM)
  view.setUint16(22, 1, true) // NumChannels
  view.setUint32(24, sampleRate, true) // SampleRate
  view.setUint32(28, sampleRate * 2, true) // ByteRate
  view.setUint16(32, 2, true) // BlockAlign
  view.setUint16(34, 16, true) // BitsPerSample
  writeString(view, 36, "data")
  view.setUint32(40, bufferLength * 2, true) // Subchunk2Size

  // Write audio samples
  let offset = 44
  for (const sample of samples) {
    for (let i = 0; i < sample.length; i++, offset += 2) {
      const s = Math.max(-1, Math.min(1, sample[i] ?? 0))
      view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7fff, true)
    }
  }

  return new Blob([view], { type: "audio/wav" })
}

const writeString = (view: DataView, offset: number, str: string): void => {
  for (let i = 0; i < str.length; i++) {
    view.setUint8(offset + i, str.charCodeAt(i))
  }
}

const parseTimestamp = (timestamp: string): number | undefined => {
  const [hms, millisecondsString] = timestamp.split(",")
  if (!hms || !millisecondsString) return undefined

  const [hours, minutes, seconds] = hms.split(":").map(Number)
  const milliseconds = Number(millisecondsString)

  if (
    hours === undefined ||
    minutes === undefined ||
    seconds === undefined ||
    milliseconds === undefined
  ) {
    return undefined
  }

  const totalSeconds =
    hours * 3600 + minutes * 60 + seconds + milliseconds / 1000
  return totalSeconds
}

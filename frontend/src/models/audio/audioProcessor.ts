import { createAudioFile } from "./createAudioFile"
import { parseTimestamp } from "./parseTimestamp"

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

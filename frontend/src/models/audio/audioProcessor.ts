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
  private readonly SILENCE_THRESHOLD = 0.01
  private readonly SILENCE_DURATION_THRESHOLD: number
  private readonly PROCESSOR_INTERVAL = 60 * 1000 // 60 seconds

  private audioData: Float32Array[] = []
  private lastProcessedArrayIndex = 0
  private lastProcessedInternalIndex = 0
  private processorTimer: NodeJS.Timeout | null = null
  private silenceCounter = 0
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

    const dataToProcess = this.getUnprocessedData()
    if (dataToProcess.length === 0 || this.isAllSilent(dataToProcess)) return

    const file = createAudioFile(dataToProcess, this.sampleRate, true)
    const currentIndices = {
      arrayIndex: this.audioData.length,
      internalIndex: 0,
    }

    const timestamp = await this.processorCallback({
      data: file,
      incomplete: isIncomplete,
    })

    this.updateProcessedIndices(timestamp, currentIndices)
  }

  private getUnprocessedData(): Float32Array[] {
    const dataToProcess: Float32Array[] = []
    const firstChunk = this.audioData[this.lastProcessedArrayIndex]

    if (firstChunk) {
      dataToProcess.push(firstChunk.slice(this.lastProcessedInternalIndex))
    }

    dataToProcess.push(
      ...this.audioData.slice(this.lastProcessedArrayIndex + 1)
    )
    return dataToProcess
  }

  private isAllSilent(chunks: Float32Array[]): boolean {
    return chunks.every((chunk) => this.isSilent(chunk))
  }

  private updateProcessedIndices(
    timestamp: string | undefined,
    fallbackIndices: { arrayIndex: number; internalIndex: number }
  ): void {
    if (!timestamp) {
      this.lastProcessedArrayIndex = fallbackIndices.arrayIndex
      this.lastProcessedInternalIndex = fallbackIndices.internalIndex
      return
    }

    const processedSeconds = parseTimestamp(timestamp)
    if (processedSeconds === undefined) {
      this.lastProcessedArrayIndex = fallbackIndices.arrayIndex
      this.lastProcessedInternalIndex = fallbackIndices.internalIndex
      return
    }

    this.calculateNewIndices(processedSeconds)
  }

  private calculateNewIndices(processedSeconds: number): void {
    const processedSamples = Math.floor(processedSeconds * this.sampleRate)
    let totalSamples = this.lastProcessedInternalIndex

    for (let i = this.lastProcessedArrayIndex; i < this.audioData.length; i++) {
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
    }, this.PROCESSOR_INTERVAL)
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

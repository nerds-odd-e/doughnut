import { createAudioFile } from "./createAudioFile"
import { AudioBuffer, isSilent } from "./audioBuffer"

export interface AudioProcessor {
  processAudioData(newData: Float32Array[]): void
  getAudioData(): Float32Array[]
  start(): void
  stop(): Promise<File>
  tryFlush(): Promise<void>
}

export interface AudioChunk {
  data: File
  isMidSpeech: boolean
}

class AudioProcessorImpl implements AudioProcessor {
  private readonly SILENCE_DURATION_THRESHOLD: number
  private readonly PROCESSOR_INTERVAL = 60 * 1000 // 60 seconds

  private audioBuffer = new AudioBuffer()
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

  private async processDataChunk(isMidSpeech = true): Promise<void> {
    const file = this.audioBuffer.tryGetProcessableData(this.sampleRate)
    if (!file) return

    const timestamp = await this.processorCallback({
      data: file,
      isMidSpeech,
    })

    this.audioBuffer.updateProcessedIndices(timestamp, this.sampleRate)
  }

  private startTimer(): void {
    this.processorTimer = setInterval(() => {
      this.processAndCallback()
    }, this.PROCESSOR_INTERVAL)
  }

  private async processAndCallback(isMidSpeech = true): Promise<void> {
    if (this.isProcessing) return

    this.isProcessing = true
    try {
      await this.processDataChunk(isMidSpeech)
    } finally {
      this.isProcessing = false
    }
  }

  processAudioData(newData: Float32Array[]): void {
    for (const chunk of newData) {
      if (isSilent(chunk)) {
        this.silenceCounter += chunk.length
        if (this.silenceCounter >= this.SILENCE_DURATION_THRESHOLD) {
          this.tryFlush()
          this.silenceCounter = 0
        }
      } else {
        this.silenceCounter = 0
      }
      this.audioBuffer.push(chunk)
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
    if (this.audioBuffer.hasUnprocessedData()) {
      this.isProcessing = true
      try {
        await this.processDataChunk(false)
      } finally {
        this.isProcessing = false
      }
    }

    return createAudioFile(this.audioBuffer.getAll(), this.sampleRate, false)
  }

  getAudioData(): Float32Array[] {
    return this.audioBuffer.getAll()
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

import { AudioBuffer } from "./audioBuffer"

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
  private readonly PROCESSOR_INTERVAL = 60 * 1000 // 60 seconds

  private processorTimer: NodeJS.Timeout | null = null
  private isProcessing = false

  constructor(
    private readonly audioBuffer: AudioBuffer,
    private readonly processorCallback: (
      chunk: AudioChunk
    ) => Promise<string | undefined>
  ) {}

  private async processDataChunk(isMidSpeech = true): Promise<void> {
    const file = this.audioBuffer.tryGetProcessableData()
    if (!file) return

    const timestamp = await this.processorCallback({
      data: file,
      isMidSpeech,
    })

    this.audioBuffer.updateProcessedIndices(timestamp)
  }

  private startTimer(): void {
    this.processorTimer = setInterval(() => {
      this.processAndCallback(true)
    }, this.PROCESSOR_INTERVAL)
  }

  private async processAndCallback(isMidSpeech: boolean): Promise<void> {
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
      this.audioBuffer.processNewChunk(chunk, () => this.tryFlush())
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

    return this.audioBuffer.createFinalAudioFile()
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
  const audioBuffer = new AudioBuffer(sampleRate)
  return new AudioProcessorImpl(audioBuffer, processorCallback)
}

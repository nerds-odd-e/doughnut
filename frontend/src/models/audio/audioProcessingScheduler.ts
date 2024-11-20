import { AudioBuffer } from "./audioBuffer"

export interface AudioProcessingScheduler {
  start(): void
  stop(): Promise<File>
  tryFlush(): Promise<void>
}

export interface AudioChunk {
  data: File
  isMidSpeech: boolean
}

class AudioProcessingSchedulerImpl implements AudioProcessingScheduler {
  private readonly PROCESSOR_INTERVAL = 60 * 1000 // 60 seconds

  private processorTimer: NodeJS.Timeout | null = null
  private isProcessing = false

  constructor(
    protected readonly audioBuffer: AudioBuffer,
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

  async tryFlush(): Promise<void> {
    if (this.processorTimer) {
      clearInterval(this.processorTimer)
      this.startTimer()
    }
    await this.processAndCallback(false)
  }
}

export const wireAudioProcessingScheduler = (
  audioBuffer: AudioBuffer,
  processorCallback: (chunk: AudioChunk) => Promise<string | undefined>
): AudioProcessingScheduler => {
  const scheduler = new AudioProcessingSchedulerImpl(
    audioBuffer,
    processorCallback
  )
  audioBuffer.setOnSilenceThresholdReached(() => scheduler.tryFlush())
  return scheduler
}

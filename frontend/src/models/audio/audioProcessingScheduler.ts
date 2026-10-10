import type { AudioBuffer } from "./audioReceiver"

export interface AudioProcessingScheduler {
  start(): void
  stop(): Promise<void>
  tryFlush(): Promise<void>
}

export interface AudioChunk {
  data: File
  isMidSpeech: boolean
}

export interface AudioProcessingSchedulerOptions {
  convertOnlyAtStop?: boolean
}

class AudioProcessingSchedulerImpl implements AudioProcessingScheduler {
  private readonly PROCESSOR_INTERVAL = 20 * 1000 // 20 seconds

  private processorTimer: NodeJS.Timeout | null = null
  private processing: Promise<void> | null = null

  constructor(
    protected readonly audioBuffer: AudioBuffer,
    private readonly processorCallback: (
      chunk: AudioChunk
    ) => Promise<string | undefined>,
    private readonly convertOnlyAtStop = false
  ) {}

  private startTimer(): void {
    this.processorTimer = setInterval(() => {
      this.processAndCallback(true)
    }, this.PROCESSOR_INTERVAL)
  }

  private async processAndCallback(isMidSpeech: boolean): Promise<void> {
    if (this.processing) return
    if (isMidSpeech && this.convertOnlyAtStop) return

    this.processing = this.audioBuffer
      .processUnprocessedData(this.processorCallback, isMidSpeech)
      .catch(() => {
        // A failed conversion leaves its audio unprocessed for the next one.
      })
      .finally(() => {
        this.processing = null
      })
    await this.processing
  }

  start(): void {
    if (!this.convertOnlyAtStop) {
      this.startTimer()
    }
  }

  async stop(): Promise<void> {
    if (this.processorTimer) {
      clearInterval(this.processorTimer)
      this.processorTimer = null
    }

    // Wait for any ongoing processing to complete
    while (this.processing) {
      await this.processing
    }

    // Process any remaining data
    if (this.audioBuffer.hasUnprocessedData()) {
      await this.processAndCallback(false)
    }
  }

  async tryFlush(): Promise<void> {
    if (this.processorTimer) {
      clearInterval(this.processorTimer)
      this.startTimer()
    }
    await this.processAndCallback(true)
  }
}

export const wireAudioProcessingScheduler = (
  audioBuffer: AudioBuffer,
  processorCallback: (chunk: AudioChunk) => Promise<string | undefined>,
  options?: AudioProcessingSchedulerOptions
): AudioProcessingScheduler => {
  const convertOnlyAtStop = options?.convertOnlyAtStop ?? false
  const scheduler = new AudioProcessingSchedulerImpl(
    audioBuffer,
    processorCallback,
    convertOnlyAtStop
  )
  if (!convertOnlyAtStop) {
    audioBuffer.setOnSilenceThresholdReached(() => scheduler.tryFlush())
  }
  return scheduler
}

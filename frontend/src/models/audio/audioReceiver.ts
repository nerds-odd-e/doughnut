export interface AudioBuffer {
  hasUnprocessedData: () => boolean
  processUnprocessedData: (
    processorCallback: (chunk: AudioChunk) => Promise<string | undefined>,
    isMidSpeech?: boolean
  ) => Promise<void>
  setOnSilenceThresholdReached: (callback: () => void) => void
  createFinalAudioFile(): File
}

export interface AudioReceiver {
  connect: (mediaStream: MediaStream) => Promise<void>
  disconnect: () => void
  getBuffer: () => AudioBuffer
  getCurrentAverageSample: () => number
}

import { createRawSampleAudioReceiver } from "./rawSamples/rawSampleReceiver"
import type { AudioChunk } from "./audioProcessingScheduler"

export const createAudioReceiver = () => createRawSampleAudioReceiver()

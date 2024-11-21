export interface AudioBuffer {
  receiveAudioData: (data: Float32Array[]) => void
  hasUnprocessedData: () => boolean
  processDataChunk: (
    processorCallback: (chunk: AudioChunk) => Promise<string | undefined>,
    isMidSpeech?: boolean
  ) => Promise<void>
  getCurrentAverageSample: () => number
  setOnSilenceThresholdReached: (callback: () => void) => void
  createFinalAudioFile(): File
}

export interface AudioReceiver {
  initialize: (deviceId?: string) => Promise<void>
  disconnect: () => void
  isInitialized: () => boolean
  reconnect: (deviceId: string) => Promise<void>
  getBuffer: () => AudioBuffer
}

import { createRawSampleAudioReceiver } from "./rawSamples/rawSampleReceiver"
import type { AudioChunk } from "./audioProcessingScheduler"

export const createAudioReceiver = () => {
  return createRawSampleAudioReceiver()
}

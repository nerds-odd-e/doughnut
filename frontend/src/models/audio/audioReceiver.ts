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
  connect: (mediaStream: MediaStream) => Promise<void>
  disconnect: () => void
  getBuffer: () => AudioBuffer
}

import { createRawSampleAudioReceiver } from "./rawSamples/rawSampleReceiver"
import type { AudioChunk } from "./audioProcessingScheduler"

export const createAudioReceiver = () => {
  return createRawSampleAudioReceiver()
}

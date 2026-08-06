import {
  type AudioChunk,
  wireAudioProcessingScheduler,
} from "@/models/audio/audioProcessingScheduler"
import { createAudioBuffer } from "@/models/audio/rawSamples/rawSampleAudioBuffer"

export const createBufferAndScheduler = (
  sampleRate: number,
  processorCallback: (chunk: AudioChunk) => Promise<string | undefined>
) => {
  const audioBuffer = createAudioBuffer(sampleRate)
  const scheduler = wireAudioProcessingScheduler(audioBuffer, processorCallback)
  return { audioBuffer, scheduler }
}

export type { AudioChunk }

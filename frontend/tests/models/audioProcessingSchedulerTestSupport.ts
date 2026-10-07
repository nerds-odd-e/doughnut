import {
  type AudioChunk,
  type AudioProcessingSchedulerOptions,
  wireAudioProcessingScheduler,
} from "@/models/audio/audioProcessingScheduler"
import { createAudioBuffer } from "@/models/audio/rawSamples/rawSampleAudioBuffer"

export const createBufferAndScheduler = (
  sampleRate: number,
  processorCallback: (chunk: AudioChunk) => Promise<string | undefined>,
  options?: AudioProcessingSchedulerOptions
) => {
  const audioBuffer = createAudioBuffer(sampleRate)
  const scheduler = wireAudioProcessingScheduler(
    audioBuffer,
    processorCallback,
    options
  )
  return { audioBuffer, scheduler }
}

export const oneSecondOfSound = () => new Float32Array(44100).fill(0.5)

export const wavSizeOfSeconds = (seconds: number) => 44 + seconds * 44100 * 2

export const sentFile = (
  callback: { mock: { calls: unknown[][] } },
  call: number
) => (callback.mock.calls[call]![0] as AudioChunk).data

export type { AudioChunk }

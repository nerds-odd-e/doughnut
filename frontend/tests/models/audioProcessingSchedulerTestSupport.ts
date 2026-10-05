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

export const oneSecondOfSound = () => new Float32Array(44100).fill(0.5)

export const wavSizeOfSeconds = (seconds: number) => 44 + seconds * 44100 * 2

export const sentFile = (
  callback: { mock: { calls: unknown[][] } },
  call: number
) => (callback.mock.calls[call]![0] as AudioChunk).data

export type { AudioChunk }

import { describe, it, expect } from "vitest"
import { AudioBuffer } from "@/models/audio/audioBuffer"

describe("AudioBuffer", () => {
  it("should process non-silent audio data", () => {
    const audioBuffer = new AudioBuffer(44100)

    const nonSilentData = [new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])]
    audioBuffer.processAudioData(nonSilentData)
    expect(audioBuffer.getAll().length).toBe(1)
    expect(audioBuffer.getAll()[0]).toEqual(nonSilentData[0])
  })

  it("should replace silent audio data with minimal data", () => {
    const audioBuffer = new AudioBuffer(44100)

    const silentData = [new Float32Array([0, 0, 0, 0, 0])]
    audioBuffer.processAudioData(silentData)
    expect(audioBuffer.getAll().length).toBe(1)
    expect(audioBuffer.getAll()[0]).toEqual(new Float32Array(5))
  })
})

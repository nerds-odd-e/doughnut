import { describe, it, expect, vi } from "vitest"
import { AudioBuffer } from "@/models/audio/audioBuffer"

describe("AudioBuffer", () => {
  it("should process non-silent audio data", () => {
    const audioBuffer = new AudioBuffer(44100)
    const nonSilentData = [new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])]
    audioBuffer.receiveAudioData(nonSilentData)
    expect(audioBuffer.getAll().length).toBe(1)
    expect(audioBuffer.getAll()[0]).toEqual(nonSilentData[0])
  })

  it("should detect silent audio data", () => {
    const audioBuffer = new AudioBuffer(44100)
    const silentData = [new Float32Array([0, 0, 0, 0, 0])]
    audioBuffer.receiveAudioData(silentData)
    expect(audioBuffer.getAll().length).toBe(1)
    expect(audioBuffer.getAll()[0]).toEqual(silentData[0])
  })

  it("should trigger silence callback after threshold duration", () => {
    const audioBuffer = new AudioBuffer(44100)
    const mockCallback = vi.fn()
    audioBuffer.setOnSilenceThresholdReached(mockCallback)

    // Create 3 seconds of silent data (threshold is 3 seconds)
    const silentData = new Float32Array(44100 * 3).fill(0)
    audioBuffer.receiveAudioData([silentData])

    expect(mockCallback).toHaveBeenCalledTimes(1)
  })

  it("should reset silence counter when non-silent data is received", () => {
    const audioBuffer = new AudioBuffer(44100)
    const mockCallback = vi.fn()
    audioBuffer.setOnSilenceThresholdReached(mockCallback)

    // Add 2 seconds of silent data
    const silentData = new Float32Array(44100 * 2).fill(0)
    audioBuffer.receiveAudioData([silentData])

    // Add non-silent data
    const nonSilentData = new Float32Array(44100).fill(0.5)
    audioBuffer.receiveAudioData([nonSilentData])

    // Add 2 more seconds of silent data (shouldn't trigger callback)
    audioBuffer.receiveAudioData([silentData])

    expect(mockCallback).not.toHaveBeenCalled()
  })

  it("should update processed indices based on timestamp", () => {
    const audioBuffer = new AudioBuffer(44100)

    // Add 2 seconds of data
    const data = new Float32Array(44100 * 2).fill(0.5)
    audioBuffer.receiveAudioData([data])

    // Update to process first second
    audioBuffer.updateProcessedIndices("00:00:01,000")

    // Try to get processable data - should only get the second half
    const file = audioBuffer.tryGetProcessableData()
    expect(file).toBeTruthy()
  })

  it("should handle invalid timestamp gracefully", () => {
    const audioBuffer = new AudioBuffer(44100)
    const data = new Float32Array(44100).fill(0.5)
    audioBuffer.receiveAudioData([data])

    audioBuffer.updateProcessedIndices("invalid")

    // Should process all data despite invalid timestamp
    expect(audioBuffer.hasUnprocessedData()).toBe(false)
  })

  it("should correctly identify unprocessed data status", () => {
    const audioBuffer = new AudioBuffer(44100)

    expect(audioBuffer.hasUnprocessedData()).toBe(false)

    const data = new Float32Array(44100).fill(0.5)
    audioBuffer.receiveAudioData([data])

    expect(audioBuffer.hasUnprocessedData()).toBe(true)

    audioBuffer.updateProcessedIndices("00:00:01,000")

    expect(audioBuffer.hasUnprocessedData()).toBe(false)
  })
})

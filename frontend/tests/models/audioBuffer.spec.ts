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

  it("should process data based on timestamp", async () => {
    const audioBuffer = new AudioBuffer(44100)

    // Add 2 seconds of data
    const data = new Float32Array(44100 * 2).fill(0.5)
    audioBuffer.receiveAudioData([data])

    // Process first second
    const processorCallback = vi.fn().mockResolvedValue("00:00:01,000")
    await audioBuffer.processDataChunk(processorCallback)

    // Process remaining data
    await audioBuffer.processDataChunk(processorCallback)
    expect(processorCallback).toHaveBeenCalledTimes(2)
  })

  it("should handle invalid timestamp in processor callback gracefully", async () => {
    const audioBuffer = new AudioBuffer(44100)
    const data = new Float32Array(44100).fill(0.5)
    audioBuffer.receiveAudioData([data])

    const processorCallback = vi.fn().mockResolvedValue("invalid")
    await audioBuffer.processDataChunk(processorCallback)

    // Should have attempted to process the data
    expect(processorCallback).toHaveBeenCalled()
  })

  it("should correctly process data chunks", async () => {
    const audioBuffer = new AudioBuffer(44100)

    // Initially no unprocessed data
    const processorCallback = vi.fn().mockResolvedValue("00:00:01,000")
    await audioBuffer.processDataChunk(processorCallback)
    expect(processorCallback).not.toHaveBeenCalled()

    // Add some data
    const data = new Float32Array(44100).fill(0.5)
    audioBuffer.receiveAudioData([data])

    // Process the data
    await audioBuffer.processDataChunk(processorCallback)
    expect(processorCallback).toHaveBeenCalled()
  })

  it("should not miss data that arrives during processing", async () => {
    const audioBuffer = new AudioBuffer(44100)

    // Add initial 1 second of data
    const initialData = new Float32Array(44100).fill(0.5)
    audioBuffer.receiveAudioData([initialData])

    // Simulate slow processing with delayed response
    const processorCallback = vi.fn().mockImplementation(async () => {
      // While we're "processing", new data arrives
      const newData = new Float32Array(44100).fill(0.7)
      audioBuffer.receiveAudioData([newData])

      return undefined
    })

    // Process the first chunk
    await audioBuffer.processDataChunk(processorCallback)

    // The second chunk should still be available for processing
    expect(audioBuffer.hasUnprocessedData()).toBe(true)

    // Process the second chunk
    await audioBuffer.processDataChunk(processorCallback)

    // Verify both chunks were processed
    expect(processorCallback).toHaveBeenCalledTimes(2)
  })
})

import { describe, it, expect, vi } from "vitest"
import {
  createAudioProcessor,
  type AudioChunk,
} from "@/models/audio/audioProcessor"

describe("AudioProcessor", () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  it("should be defined", () => {
    expect(createAudioProcessor).toBeDefined()
  })

  it("should process non-silent audio data", () => {
    const mockCallback = vi.fn()
    const processor = createAudioProcessor(44100, mockCallback)

    const nonSilentData = [new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])]
    processor.processAudioData(nonSilentData)
    expect(processor.getAudioData().length).toBe(1)
    expect(processor.getAudioData()[0]).toEqual(nonSilentData[0])
  })

  it("should replace silent audio data with minimal data", () => {
    const mockCallback = vi.fn()
    const processor = createAudioProcessor(44100, mockCallback)

    const silentData = [new Float32Array([0, 0, 0, 0, 0])]
    processor.processAudioData(silentData)
    expect(processor.getAudioData().length).toBe(1)
    expect(processor.getAudioData()[0]).toEqual(new Float32Array(5))
  })

  it("should not call processorCallback if data is all silent", () => {
    const mockCallback = vi.fn()
    const processor = createAudioProcessor(44100, mockCallback)

    const silentData = [new Float32Array([0, 0, 0, 0, 0])]
    processor.processAudioData(silentData)
    processor.start()

    // Fast-forward the timer
    vi.advanceTimersByTime(60 * 1000)

    expect(mockCallback).not.toHaveBeenCalled()
    processor.stop()
  })

  it("should call processorCallback if data is not all silent", () => {
    const mockCallback = vi.fn()
    const processor = createAudioProcessor(44100, mockCallback)

    const nonSilentData = [new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])]
    processor.processAudioData(nonSilentData)
    processor.start()

    // Fast-forward the timer
    vi.advanceTimersByTime(60 * 1000)

    expect(mockCallback).toHaveBeenCalled()
    processor.stop()
  })

  it("should process data and reset timer when 2 seconds of silence is detected", async () => {
    const mockCallback = vi.fn()
    const sampleRate = 44100
    const processor = createAudioProcessor(sampleRate, mockCallback)

    const nonSilentData = new Float32Array(sampleRate).fill(0.5)
    const silentData = new Float32Array(sampleRate * 3).fill(0)
    const moreNonSilentData = new Float32Array(sampleRate).fill(0.5)

    processor.processAudioData([nonSilentData])
    processor.start()

    // Fast-forward less than a minute
    vi.advanceTimersByTime(30 * 1000)

    // Process silent data
    processor.processAudioData([silentData])

    // Fast-forward 3 seconds to trigger silence detection
    vi.advanceTimersByTime(3000)
    // Wait for any pending processing
    await Promise.resolve()

    // Check if the callback was called with the non-silent data
    expect(mockCallback).toHaveBeenCalledTimes(1)

    // Reset mock to check if it's called again
    mockCallback.mockClear()

    // Process more non-silent data
    processor.processAudioData([moreNonSilentData])

    // Fast-forward the timer to just before the next minute
    vi.advanceTimersByTime(27 * 1000)

    // The callback should not have been called again yet
    expect(mockCallback).not.toHaveBeenCalled()

    // Fast-forward to complete the minute
    vi.advanceTimersByTime(33 * 1000)
    // Wait for any pending processing
    await Promise.resolve()

    // Now the callback should have been called with the new non-silent data
    expect(mockCallback).toHaveBeenCalledTimes(1)

    await processor.stop()
  })

  it("should flush remaining data and call processorCallback", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const processor = createAudioProcessor(44100, mockCallback)

    const nonSilentData = [new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])]
    processor.processAudioData(nonSilentData)
    processor.start()

    vi.advanceTimersByTime(30 * 1000)

    await processor.tryFlush()

    expect(mockCallback).toHaveBeenCalledTimes(1)
    const callArgument = mockCallback.mock.calls[0]?.[0] as AudioChunk
    expect(callArgument.data).toBeInstanceOf(File)
    expect(callArgument.data.name).toMatch(/^recorded_audio_partial_.*\.wav$/)
  })

  it("should not call processorCallback on tryFlush if no new data", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const processor = createAudioProcessor(44100, mockCallback)

    processor.start()
    vi.advanceTimersByTime(65 * 1000)
    mockCallback.mockClear()

    await processor.tryFlush()

    expect(mockCallback).not.toHaveBeenCalled()
  })

  it("should mark chunk as incomplete when processing due to timer", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const processor = createAudioProcessor(44100, mockCallback)

    const nonSilentData = [new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])]
    processor.processAudioData(nonSilentData)
    processor.start()

    // Fast-forward to trigger timer
    vi.advanceTimersByTime(60 * 1000)

    expect(mockCallback).toHaveBeenCalledWith(
      expect.objectContaining({
        incomplete: true,
        data: expect.any(File),
      })
    )
  })

  it("should mark chunk as complete when stopping", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const processor = createAudioProcessor(44100, mockCallback)

    const nonSilentData = [new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])]
    processor.processAudioData(nonSilentData)
    processor.start()

    await processor.stop()

    expect(mockCallback).toHaveBeenLastCalledWith(
      expect.objectContaining({
        incomplete: false,
        data: expect.any(File),
      })
    )
  })

  it("should mark chunk as not incomplete when silence triggers callback", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const processor = createAudioProcessor(44100, mockCallback)

    // Create 1 second of non-silent data followed by 3 seconds of silence
    const nonSilentData = new Float32Array(44100).fill(0.5)
    const silentData = new Float32Array(44100 * 3).fill(0)

    processor.processAudioData([nonSilentData])
    processor.start()

    // Process silent data to trigger silence detection
    processor.processAudioData([silentData])

    expect(mockCallback).toHaveBeenCalledWith(
      expect.objectContaining({
        incomplete: false,
        data: expect.any(File),
      })
    )

    // Verify it was called exactly once
    expect(mockCallback).toHaveBeenCalledTimes(1)
  })

  it("should adjust lastProcessedIndex based on returned timestamp", async () => {
    const mockCallback = vi.fn().mockResolvedValue("00:00:01,500") // 1.5 seconds
    const processor = createAudioProcessor(44100, mockCallback)

    // Create 3 seconds of non-silent data
    const nonSilentData = new Float32Array(44100 * 3).fill(0.5)
    processor.processAudioData([nonSilentData])
    processor.start()

    // Process first chunk
    await processor.tryFlush()

    // The lastProcessedIndex should be at 1.5 seconds worth of samples
    expect(mockCallback).toHaveBeenCalledTimes(1)

    // Process remaining data
    await processor.tryFlush()
    expect(mockCallback).toHaveBeenCalledTimes(2)
  })

  it("should handle invalid timestamp gracefully", async () => {
    const mockCallback = vi.fn().mockResolvedValue("invalid")
    const processor = createAudioProcessor(44100, mockCallback)

    const nonSilentData = new Float32Array(44100).fill(0.5)
    processor.processAudioData([nonSilentData])
    processor.start()

    await processor.tryFlush()
    // Should process all data despite invalid timestamp
    expect(processor.getAudioData().length).toBe(1)
  })

  it("should correctly process partial chunks based on lastProcessedInternalIndex", async () => {
    const mockCallback = vi.fn().mockResolvedValue("00:00:00,500") // 0.5 seconds
    const processor = createAudioProcessor(44100, mockCallback)

    // Create 1 second of non-silent data
    const nonSilentData = new Float32Array(44100).fill(0.5)
    processor.processAudioData([nonSilentData])
    processor.start()

    // Process first chunk
    await processor.tryFlush()

    // Should process only first 0.5 seconds
    const firstCall = mockCallback.mock.calls[0]?.[0] as AudioChunk
    const firstFileSize = firstCall.data.size

    // Process remaining data
    await processor.tryFlush()

    // Second chunk should be smaller than first (remaining 0.5 seconds)
    const secondCall = mockCallback.mock.calls[1]?.[0] as AudioChunk
    expect(secondCall.data.size).toBeLessThan(firstFileSize)
    expect(mockCallback).toHaveBeenCalledTimes(2)
  })

  it("should accumulate processed samples on top of existing indices", async () => {
    const mockCallback = vi
      .fn()
      .mockResolvedValueOnce("00:00:00,500") // First call: 0.5 seconds
      .mockResolvedValueOnce("00:00:01,000") // Second call: 1 second from the remaining data

    const processor = createAudioProcessor(44100, mockCallback)

    // Create 2 seconds of non-silent data
    const nonSilentData = new Float32Array(44100 * 2).fill(0.5)
    processor.processAudioData([nonSilentData])
    processor.start()

    // Process first chunk - should process 0.5 seconds
    await processor.tryFlush()
    expect(mockCallback).toHaveBeenCalledTimes(1)

    // Add more data
    const additionalData = new Float32Array(44100).fill(0.5)
    processor.processAudioData([additionalData])

    // Process next chunk - should process 1 second from the remaining data
    await processor.tryFlush()
    expect(mockCallback).toHaveBeenCalledTimes(2)

    // Process final chunk - should process the rest
    await processor.tryFlush()
    expect(mockCallback).toHaveBeenCalledTimes(3)
  })

  it("should not process in parallel when multiple calls are made", async () => {
    const mockCallback = vi.fn().mockImplementation(async () => {
      await new Promise((resolve) => setTimeout(resolve, 100))
      return "00:00:00,500"
    })

    const processor = createAudioProcessor(44100, mockCallback)
    const nonSilentData = new Float32Array(44100).fill(0.5)
    processor.processAudioData([nonSilentData])

    const promise1 = processor.tryFlush()
    const promise2 = processor.tryFlush()
    const promise3 = processor.tryFlush()

    vi.advanceTimersByTime(100)
    await Promise.all([promise1, promise2, promise3])

    expect(mockCallback).toHaveBeenCalledTimes(1)
  })
})

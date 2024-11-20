import { describe, it, expect, vi } from "vitest"
import {
  type AudioChunk,
  wireAudioProcessingScheduler,
} from "@/models/audio/audioProcessingScheduler"
import { AudioBuffer } from "@/models/audio/audioBuffer"

const createBufferAndScheduler = (
  sampleRate: number,
  processorCallback: (chunk: AudioChunk) => Promise<string | undefined>
) => {
  const audioBuffer = new AudioBuffer(sampleRate)
  const scheduler = wireAudioProcessingScheduler(audioBuffer, processorCallback)
  return { audioBuffer, scheduler }
}

describe("AudioProcessingScheduler", () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  it("should process non-silent audio data", () => {
    const mockCallback = vi.fn()
    const { audioBuffer } = createBufferAndScheduler(44100, mockCallback)

    const nonSilentData = [new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])]
    audioBuffer.processAudioData(nonSilentData)
    expect(audioBuffer.getAll().length).toBe(1)
    expect(audioBuffer.getAll()[0]).toEqual(nonSilentData[0])
  })

  it("should replace silent audio data with minimal data", () => {
    const mockCallback = vi.fn()
    const { audioBuffer } = createBufferAndScheduler(44100, mockCallback)

    const silentData = [new Float32Array([0, 0, 0, 0, 0])]
    audioBuffer.processAudioData(silentData)
    expect(audioBuffer.getAll().length).toBe(1)
    expect(audioBuffer.getAll()[0]).toEqual(new Float32Array(5))
  })

  it("should not call processorCallback if data is all silent", () => {
    const mockCallback = vi.fn()
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    const silentData = [new Float32Array([0, 0, 0, 0, 0])]
    audioBuffer.processAudioData(silentData)
    scheduler.start()

    // Fast-forward the timer
    vi.advanceTimersByTime(60 * 1000)

    expect(mockCallback).not.toHaveBeenCalled()
    scheduler.stop()
  })

  it("should call processorCallback if data is not all silent", () => {
    const mockCallback = vi.fn()
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    const nonSilentData = [new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])]
    audioBuffer.processAudioData(nonSilentData)
    scheduler.start()

    // Fast-forward the timer
    vi.advanceTimersByTime(60 * 1000)

    expect(mockCallback).toHaveBeenCalled()
    scheduler.stop()
  })

  it("should process data and reset timer when 2 seconds of silence is detected", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const sampleRate = 44100
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      sampleRate,
      mockCallback
    )

    const nonSilentData = new Float32Array(sampleRate).fill(0.5)
    const silentData = new Float32Array(sampleRate * 3).fill(0)
    const moreNonSilentData = new Float32Array(sampleRate).fill(0.5)

    // Process initial non-silent data
    audioBuffer.processAudioData([nonSilentData])
    scheduler.start()

    // Process silent data that should trigger the callback
    audioBuffer.processAudioData([silentData])

    // Advance time to allow silence detection to trigger
    await vi.advanceTimersByTimeAsync(3000)

    // Check if the callback was called with the non-silent data
    expect(mockCallback).toHaveBeenCalledTimes(1)
    expect(mockCallback).toHaveBeenLastCalledWith(
      expect.objectContaining({
        isMidSpeech: false,
      })
    )

    // Reset mock to check if it's called again
    mockCallback.mockClear()

    // Process more non-silent data
    audioBuffer.processAudioData([moreNonSilentData])

    // Advance time to trigger the regular timer
    await vi.advanceTimersByTimeAsync(60 * 1000)

    // Now the callback should have been called with the new non-silent data
    expect(mockCallback).toHaveBeenCalledTimes(1)
    expect(mockCallback).toHaveBeenLastCalledWith(
      expect.objectContaining({
        isMidSpeech: true,
      })
    )

    // Clean up
    const stopPromise = scheduler.stop()
    await vi.runAllTimersAsync()
    await stopPromise
  })

  it("should flush remaining data and call processorCallback", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    const nonSilentData = [new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])]
    audioBuffer.processAudioData(nonSilentData)
    scheduler.start()

    vi.advanceTimersByTime(30 * 1000)

    await scheduler.tryFlush()

    expect(mockCallback).toHaveBeenCalledTimes(1)
    const callArgument = mockCallback.mock.calls[0]?.[0] as AudioChunk
    expect(callArgument.data).toBeInstanceOf(File)
    expect(callArgument.data.name).toMatch(/^recorded_audio_partial_.*\.wav$/)
  })

  it("should not call processorCallback on tryFlush if no new data", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { scheduler } = createBufferAndScheduler(44100, mockCallback)

    scheduler.start()
    vi.advanceTimersByTime(65 * 1000)
    mockCallback.mockClear()

    await scheduler.tryFlush()

    expect(mockCallback).not.toHaveBeenCalled()
  })

  it("should mark chunk as isMidSpeech when processing due to timer", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    const nonSilentData = [new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])]
    audioBuffer.processAudioData(nonSilentData)
    scheduler.start()

    // Fast-forward to trigger timer
    vi.advanceTimersByTime(60 * 1000)

    expect(mockCallback).toHaveBeenCalledWith(
      expect.objectContaining({
        isMidSpeech: true,
        data: expect.any(File),
      })
    )
  })

  it("should mark chunk as complete when stopping", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    const nonSilentData = [new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])]
    audioBuffer.processAudioData(nonSilentData)
    scheduler.start()

    await scheduler.stop()

    expect(mockCallback).toHaveBeenLastCalledWith(
      expect.objectContaining({
        isMidSpeech: false,
        data: expect.any(File),
      })
    )
  })

  it("should mark chunk as not isMidSpeech when silence triggers callback", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    // Create 1 second of non-silent data followed by 3 seconds of silence
    const nonSilentData = new Float32Array(44100).fill(0.5)
    const silentData = new Float32Array(44100 * 3).fill(0)

    audioBuffer.processAudioData([nonSilentData])
    scheduler.start()

    // Process silent data to trigger silence detection
    audioBuffer.processAudioData([silentData])

    expect(mockCallback).toHaveBeenCalledWith(
      expect.objectContaining({
        isMidSpeech: false,
        data: expect.any(File),
      })
    )

    // Verify it was called exactly once
    expect(mockCallback).toHaveBeenCalledTimes(1)
  })

  it("should adjust lastProcessedIndex based on returned timestamp", async () => {
    const mockCallback = vi.fn().mockResolvedValue("00:00:01,500") // 1.5 seconds
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    // Create 3 seconds of non-silent data
    const nonSilentData = new Float32Array(44100 * 3).fill(0.5)
    audioBuffer.processAudioData([nonSilentData])
    scheduler.start()

    // Process first chunk
    await scheduler.tryFlush()

    // The lastProcessedIndex should be at 1.5 seconds worth of samples
    expect(mockCallback).toHaveBeenCalledTimes(1)

    // Process remaining data
    await scheduler.tryFlush()
    expect(mockCallback).toHaveBeenCalledTimes(2)
  })

  it("should handle invalid timestamp gracefully", async () => {
    const mockCallback = vi.fn().mockResolvedValue("invalid")
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    const nonSilentData = new Float32Array(44100).fill(0.5)
    audioBuffer.processAudioData([nonSilentData])
    scheduler.start()

    await scheduler.tryFlush()
    // Should process all data despite invalid timestamp
    expect(audioBuffer.getAll().length).toBe(1)
  })

  it("should correctly process partial chunks based on lastProcessedInternalIndex", async () => {
    const mockCallback = vi.fn().mockResolvedValue("00:00:00,500") // 0.5 seconds
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    // Create 1 second of non-silent data
    const nonSilentData = new Float32Array(44100).fill(0.5)
    audioBuffer.processAudioData([nonSilentData])
    scheduler.start()

    // Process first chunk
    await scheduler.tryFlush()

    // Should process only first 0.5 seconds
    const firstCall = mockCallback.mock.calls[0]?.[0] as AudioChunk
    const firstFileSize = firstCall.data.size

    // Process remaining data
    await scheduler.tryFlush()

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

    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    // Create 2 seconds of non-silent data
    const nonSilentData = new Float32Array(44100 * 2).fill(0.5)
    audioBuffer.processAudioData([nonSilentData])
    scheduler.start()

    // Process first chunk - should process 0.5 seconds
    await scheduler.tryFlush()
    expect(mockCallback).toHaveBeenCalledTimes(1)

    // Add more data
    const additionalData = new Float32Array(44100).fill(0.5)
    audioBuffer.processAudioData([additionalData])

    // Process next chunk - should process 1 second from the remaining data
    await scheduler.tryFlush()
    expect(mockCallback).toHaveBeenCalledTimes(2)

    // Process final chunk - should process the rest
    await scheduler.tryFlush()
    expect(mockCallback).toHaveBeenCalledTimes(3)
  })

  it("should not process in parallel when multiple calls are made", async () => {
    const mockCallback = vi.fn().mockImplementation(async () => {
      await new Promise((resolve) => setTimeout(resolve, 100))
      return "00:00:00,500"
    })

    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )
    const nonSilentData = new Float32Array(44100).fill(0.5)
    audioBuffer.processAudioData([nonSilentData])

    const promise1 = scheduler.tryFlush()
    const promise2 = scheduler.tryFlush()
    const promise3 = scheduler.tryFlush()

    vi.advanceTimersByTime(100)
    await Promise.all([promise1, promise2, promise3])

    expect(mockCallback).toHaveBeenCalledTimes(1)
  })

  it("should wait for ongoing processing to complete before stopping", async () => {
    let resolveProcessing: (() => void) | null = null
    const processingPromise = new Promise<void>((resolve) => {
      resolveProcessing = resolve
    })

    const mockCallback = vi
      .fn()
      .mockImplementationOnce(async () => {
        await processingPromise
        return "00:00:00,250" // Process 0.25 seconds
      })
      .mockImplementationOnce(async () => {
        // Second call for remaining data
        return undefined
      })

    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )
    const nonSilentData = new Float32Array(44100).fill(0.5) // 1 second of data
    audioBuffer.processAudioData([nonSilentData])

    // Start processing
    const flushPromise = scheduler.tryFlush()

    // Attempt to stop immediately
    const stopPromise = scheduler.stop()

    // Simulate processing completion
    resolveProcessing!()

    // Advance timers to handle the interval in stop()
    await vi.advanceTimersByTimeAsync(20)
    await Promise.all([flushPromise, stopPromise])

    // Should be called twice:
    // 1. First call processes 0.25 seconds
    // 2. Second call processes remaining 0.75 seconds
    expect(mockCallback).toHaveBeenCalledTimes(2)
    expect(mockCallback.mock.calls[1]?.[0]).toEqual(
      expect.objectContaining({
        isMidSpeech: false,
      })
    )
  })

  it("should process remaining data when stopping during ongoing processing", async () => {
    let resolveFirstProcessing: (() => void) | null = null
    const firstProcessingPromise = new Promise<void>((resolve) => {
      resolveFirstProcessing = resolve
    })

    const mockCallback = vi
      .fn()
      .mockImplementationOnce(async () => {
        await firstProcessingPromise
        return "00:00:00,500" // Process 0.5 seconds
      })
      .mockImplementationOnce(async () => {
        return undefined
      })

    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )
    const nonSilentData = new Float32Array(44100 * 2).fill(0.5)
    audioBuffer.processAudioData([nonSilentData])

    // Start processing first chunk
    const processingPromise = scheduler.tryFlush()

    // Give the first processing a chance to start
    await vi.advanceTimersByTimeAsync(10)

    // Start stopping while first processing is still ongoing
    const stopPromise = scheduler.stop()

    // Now resolve the first processing
    resolveFirstProcessing!()

    // Wait for everything to complete
    await vi.advanceTimersByTimeAsync(100)
    await Promise.all([processingPromise, stopPromise])

    expect(mockCallback).toHaveBeenCalledTimes(2)
    const lastCall = mockCallback.mock.calls[1]?.[0] as AudioChunk
    expect(lastCall.isMidSpeech).toBe(false)
  })

  it("should not process same data twice when stopping", async () => {
    const mockCallback = vi.fn().mockImplementation(async () => {
      // Simulate processing half of the data
      return "00:00:00,500" // 0.5 seconds
    })

    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    // Create 1 second of data
    const nonSilentData = new Float32Array(44100).fill(0.5)
    audioBuffer.processAudioData([nonSilentData])

    // Process first chunk
    await scheduler.tryFlush()

    // Clear the mock to check stop behavior
    mockCallback.mockClear()

    // Stop should only process the remaining 0.5 seconds
    await scheduler.stop()

    expect(mockCallback).toHaveBeenCalledTimes(1)
    const lastCall = mockCallback.mock.calls[0]?.[0] as AudioChunk
    // The second chunk should be smaller (only remaining data)
    expect(lastCall.data.size).toBeLessThan(44100 * 4) // Assuming 16-bit stereo WAV
  })
})

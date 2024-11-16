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

  it("should process data and reset timer when 2 seconds of silence is detected", () => {
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

    // Now the callback should have been called with the new non-silent data
    expect(mockCallback).toHaveBeenCalledTimes(1)

    processor.stop()
  })

  it("should flush remaining data and call processorCallback", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const processor = createAudioProcessor(44100, mockCallback)

    const nonSilentData = [new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])]
    processor.processAudioData(nonSilentData)
    processor.start()

    // Fast-forward less than a minute
    vi.advanceTimersByTime(30 * 1000)

    await processor.flush()

    expect(mockCallback).toHaveBeenCalledTimes(1)
    const callArgument = mockCallback.mock.calls[0]?.[0] as AudioChunk
    expect(callArgument.data).toBeInstanceOf(File)
    expect(callArgument.data.name).toMatch(/^recorded_audio_partial_.*\.wav$/)
  })

  it("should not call processorCallback on flush if no new data", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const processor = createAudioProcessor(44100, mockCallback)

    processor.start()

    // Fast-forward more than a minute
    vi.advanceTimersByTime(65 * 1000)

    mockCallback.mockClear()

    await processor.flush()

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
})

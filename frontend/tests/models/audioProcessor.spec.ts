import { describe, it, expect, vi } from "vitest"
import { createAudioProcessor } from "../../src/models/audio/audioProcessor"

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
})

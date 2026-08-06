import { describe, it, expect, vi, beforeEach } from "vitest"
import {
  type AudioChunk,
  createBufferAndScheduler,
} from "./audioProcessingSchedulerTestSupport"

describe("AudioProcessingScheduler flush", () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  it("flushes remaining data and calls processorCallback", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    audioBuffer.receiveAudioData([new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])])
    scheduler.start()
    vi.advanceTimersByTime(30 * 1000)
    await scheduler.tryFlush()

    expect(mockCallback).toHaveBeenCalledTimes(1)
    const callArgument = mockCallback.mock.calls[0]?.[0] as AudioChunk
    expect(callArgument.data).toBeInstanceOf(File)
    expect(callArgument.data.name).toMatch(/^recorded_audio_partial_.*\.wav$/)
  })

  it("does not call processorCallback on tryFlush if no new data", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { scheduler } = createBufferAndScheduler(44100, mockCallback)

    scheduler.start()
    vi.advanceTimersByTime(65 * 1000)
    mockCallback.mockClear()
    await scheduler.tryFlush()

    expect(mockCallback).not.toHaveBeenCalled()
  })

  it("marks chunk as isMidSpeech when processing due to timer", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    audioBuffer.receiveAudioData([new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])])
    scheduler.start()
    vi.advanceTimersByTime(60 * 1000)

    expect(mockCallback).toHaveBeenCalledWith(
      expect.objectContaining({
        isMidSpeech: true,
        data: expect.any(File),
      })
    )
  })

  it("marks chunk as not isMidSpeech when silence triggers callback", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    audioBuffer.receiveAudioData([new Float32Array(44100).fill(0.5)])
    scheduler.start()
    audioBuffer.receiveAudioData([new Float32Array(44100 * 3).fill(0)])

    expect(mockCallback).toHaveBeenCalledWith(
      expect.objectContaining({
        isMidSpeech: false,
        data: expect.any(File),
      })
    )
    expect(mockCallback).toHaveBeenCalledTimes(1)
  })

  it("accumulates processed samples across multiple flushes", async () => {
    const mockCallback = vi
      .fn()
      .mockResolvedValueOnce("00:00:00,500")
      .mockResolvedValueOnce("00:00:01,000")

    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    audioBuffer.receiveAudioData([new Float32Array(44100 * 2).fill(0.5)])
    scheduler.start()

    await scheduler.tryFlush()
    expect(mockCallback).toHaveBeenCalledTimes(1)

    audioBuffer.receiveAudioData([new Float32Array(44100).fill(0.5)])
    await scheduler.tryFlush()
    expect(mockCallback).toHaveBeenCalledTimes(2)

    await scheduler.tryFlush()
    expect(mockCallback).toHaveBeenCalledTimes(3)
  })

  it("does not process in parallel when multiple flush calls are made", async () => {
    const mockCallback = vi.fn().mockImplementation(async () => {
      await new Promise((resolve) => setTimeout(resolve, 100))
      return "00:00:00,500"
    })

    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )
    audioBuffer.receiveAudioData([new Float32Array(44100).fill(0.5)])

    const promise1 = scheduler.tryFlush()
    const promise2 = scheduler.tryFlush()
    const promise3 = scheduler.tryFlush()

    vi.advanceTimersByTime(100)
    await Promise.all([promise1, promise2, promise3])

    expect(mockCallback).toHaveBeenCalledTimes(1)
  })
})

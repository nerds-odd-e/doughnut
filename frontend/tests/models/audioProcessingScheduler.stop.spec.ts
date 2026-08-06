import { describe, it, expect, vi, beforeEach } from "vitest"
import {
  type AudioChunk,
  createBufferAndScheduler,
} from "./audioProcessingSchedulerTestSupport"

describe("AudioProcessingScheduler stop", () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  it("marks chunk as complete when stopping", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    audioBuffer.receiveAudioData([new Float32Array([0.5, 0.4, 0.3, 0.2, 0.1])])
    scheduler.start()
    await scheduler.stop()

    expect(mockCallback).toHaveBeenLastCalledWith(
      expect.objectContaining({
        isMidSpeech: false,
        data: expect.any(File),
      })
    )
  })

  it("waits for ongoing processing to complete before stopping", async () => {
    let resolveProcessing: (() => void) | null = null
    const processingPromise = new Promise<void>((resolve) => {
      resolveProcessing = resolve
    })

    const mockCallback = vi
      .fn()
      .mockImplementationOnce(async () => {
        await processingPromise
        return "00:00:00,250"
      })
      .mockImplementationOnce(async () => undefined)

    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )
    audioBuffer.receiveAudioData([new Float32Array(44100).fill(0.5)])

    const flushPromise = scheduler.tryFlush()
    const stopPromise = scheduler.stop()
    resolveProcessing!()

    await vi.advanceTimersByTimeAsync(20)
    await Promise.all([flushPromise, stopPromise])

    expect(mockCallback).toHaveBeenCalledTimes(2)
    expect(mockCallback.mock.calls[1]?.[0]).toEqual(
      expect.objectContaining({
        isMidSpeech: false,
      })
    )
  })

  it("processes remaining data when stopping during ongoing processing", async () => {
    let resolveFirstProcessing: (() => void) | null = null
    const firstProcessingPromise = new Promise<void>((resolve) => {
      resolveFirstProcessing = resolve
    })

    const mockCallback = vi
      .fn()
      .mockImplementationOnce(async () => {
        await firstProcessingPromise
        return "00:00:00,500"
      })
      .mockImplementationOnce(async () => undefined)

    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )
    audioBuffer.receiveAudioData([new Float32Array(44100 * 2).fill(0.5)])

    const processingPromise = scheduler.tryFlush()
    await vi.advanceTimersByTimeAsync(10)
    const stopPromise = scheduler.stop()
    resolveFirstProcessing!()

    await vi.advanceTimersByTimeAsync(100)
    await Promise.all([processingPromise, stopPromise])

    expect(mockCallback).toHaveBeenCalledTimes(2)
    const lastCall = mockCallback.mock.calls[1]?.[0] as AudioChunk
    expect(lastCall.isMidSpeech).toBe(false)
  })

  it("does not process same data twice when stopping after a partial flush", async () => {
    const mockCallback = vi.fn().mockImplementation(async () => "00:00:00,500")

    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )
    audioBuffer.receiveAudioData([new Float32Array(44100).fill(0.5)])
    await scheduler.tryFlush()
    mockCallback.mockClear()
    await scheduler.stop()

    expect(mockCallback).toHaveBeenCalledTimes(1)
    const lastCall = mockCallback.mock.calls[0]?.[0] as AudioChunk
    expect(lastCall.data.size).toBeLessThan(44100 * 4)
  })
})

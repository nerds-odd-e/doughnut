import { describe, it, expect, vi, beforeEach } from "vitest"
import {
  type AudioChunk,
  createBufferAndScheduler,
  oneSecondOfSound,
  sentFile,
  wavSizeOfSeconds,
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
    vi.advanceTimersByTime(10 * 1000)
    await scheduler.tryFlush()

    expect(mockCallback).toHaveBeenCalledTimes(1)
    const callArgument = mockCallback.mock.calls[0]?.[0] as AudioChunk
    expect(callArgument.data).toBeInstanceOf(File)
    expect(callArgument.data.name).toMatch(/^recorded_audio_.*\.wav$/)
  })

  it("does not call processorCallback on tryFlush if no new data", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { scheduler } = createBufferAndScheduler(44100, mockCallback)

    scheduler.start()
    vi.advanceTimersByTime(25 * 1000)
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
    vi.advanceTimersByTime(20 * 1000)

    expect(mockCallback).toHaveBeenCalledWith(
      expect.objectContaining({
        isMidSpeech: true,
        data: expect.any(File),
      })
    )
  })

  it("marks chunk as isMidSpeech when the author flushes", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    audioBuffer.receiveAudioData([new Float32Array(44100).fill(0.5)])
    scheduler.start()
    await scheduler.tryFlush()

    expect(mockCallback).toHaveBeenCalledWith(
      expect.objectContaining({ isMidSpeech: true })
    )
  })

  it("sends one isMidSpeech chunk for a pause, however long it lasts", async () => {
    const mockCallback = vi.fn().mockResolvedValue("00:00:00,500")
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback
    )

    audioBuffer.receiveAudioData([new Float32Array(44100).fill(0.5)])
    scheduler.start()
    audioBuffer.receiveAudioData([new Float32Array(44100 * 3).fill(0)])
    await vi.advanceTimersByTimeAsync(0)
    audioBuffer.receiveAudioData([new Float32Array(44100 * 6).fill(0)])
    await vi.advanceTimersByTimeAsync(0)

    expect(mockCallback).toHaveBeenCalledTimes(1)
    expect(mockCallback).toHaveBeenCalledWith(
      expect.objectContaining({ isMidSpeech: true })
    )
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
  describe("when a conversion fails", () => {
    it("sends the failed audio again together with the later audio", async () => {
      const mockCallback = vi
        .fn()
        .mockRejectedValueOnce(new Error("failed"))
        .mockResolvedValue(undefined)
      const { audioBuffer, scheduler } = createBufferAndScheduler(
        44100,
        mockCallback
      )

      audioBuffer.receiveAudioData([oneSecondOfSound()])
      await scheduler.tryFlush()
      audioBuffer.receiveAudioData([oneSecondOfSound()])
      await scheduler.tryFlush()

      expect(sentFile(mockCallback, 0).size).toBe(wavSizeOfSeconds(1))
      expect(sentFile(mockCallback, 1).size).toBe(wavSizeOfSeconds(2))
    })

    it("keeps the timed conversion and Flush going after the failure", async () => {
      const mockCallback = vi
        .fn()
        .mockRejectedValueOnce(new Error("failed"))
        .mockResolvedValue(undefined)
      const { audioBuffer, scheduler } = createBufferAndScheduler(
        44100,
        mockCallback
      )

      audioBuffer.receiveAudioData([oneSecondOfSound()])
      scheduler.start()
      await vi.advanceTimersByTimeAsync(20 * 1000)
      await vi.advanceTimersByTimeAsync(20 * 1000)
      audioBuffer.receiveAudioData([oneSecondOfSound()])
      await scheduler.tryFlush()

      expect(mockCallback).toHaveBeenCalledTimes(3)
    })

    it("does not send again the audio an earlier success converted", async () => {
      const mockCallback = vi
        .fn()
        .mockResolvedValueOnce("00:00:01,000")
        .mockRejectedValueOnce(new Error("failed"))
        .mockResolvedValue(undefined)
      const { audioBuffer, scheduler } = createBufferAndScheduler(
        44100,
        mockCallback
      )

      audioBuffer.receiveAudioData([oneSecondOfSound(), oneSecondOfSound()])
      await scheduler.tryFlush()
      audioBuffer.receiveAudioData([oneSecondOfSound()])
      await scheduler.tryFlush()
      audioBuffer.receiveAudioData([oneSecondOfSound()])
      await scheduler.tryFlush()

      expect(sentFile(mockCallback, 2).size).toBe(wavSizeOfSeconds(3))
    })
  })
})

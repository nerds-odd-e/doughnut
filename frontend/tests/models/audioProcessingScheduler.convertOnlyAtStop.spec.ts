import { describe, it, expect, vi, beforeEach } from "vitest"
import {
  createBufferAndScheduler,
  oneSecondOfSound,
} from "./audioProcessingSchedulerTestSupport"

describe("AudioProcessingScheduler convertOnlyAtStop", () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  it("does not call the processor on the 20 s tick", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback,
      { convertOnlyAtStop: true }
    )

    audioBuffer.receiveAudioData([oneSecondOfSound()])
    scheduler.start()
    await vi.advanceTimersByTimeAsync(20 * 1000)

    expect(mockCallback).not.toHaveBeenCalled()
  })

  it("does not call the processor when silence threshold is reached", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback,
      { convertOnlyAtStop: true }
    )

    audioBuffer.receiveAudioData([oneSecondOfSound()])
    scheduler.start()
    audioBuffer.receiveAudioData([new Float32Array(44100 * 3).fill(0)])
    await vi.advanceTimersByTimeAsync(0)

    expect(mockCallback).not.toHaveBeenCalled()
  })

  it("does not call the processor when flushed", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback,
      { convertOnlyAtStop: true }
    )

    audioBuffer.receiveAudioData([oneSecondOfSound()])
    scheduler.start()
    await scheduler.tryFlush()

    expect(mockCallback).not.toHaveBeenCalled()
  })

  it("calls the processor once at stop with isMidSpeech false", async () => {
    const mockCallback = vi.fn().mockResolvedValue(undefined)
    const { audioBuffer, scheduler } = createBufferAndScheduler(
      44100,
      mockCallback,
      { convertOnlyAtStop: true }
    )

    audioBuffer.receiveAudioData([oneSecondOfSound()])
    scheduler.start()
    await vi.advanceTimersByTimeAsync(20 * 1000)
    audioBuffer.receiveAudioData([new Float32Array(44100 * 3).fill(0)])
    await vi.advanceTimersByTimeAsync(0)
    await scheduler.stop()

    expect(mockCallback).toHaveBeenCalledTimes(1)
    expect(mockCallback).toHaveBeenCalledWith(
      expect.objectContaining({
        isMidSpeech: false,
        data: expect.any(File),
      })
    )
  })
})

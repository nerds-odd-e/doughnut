import {
  attachPdfBookViewerGeometryResampleListeners,
  createCoalescedRequestAnimationFrameEmitter,
} from "@/lib/book-reading/pdfBookViewerGeometryResample"
import { afterEach, describe, expect, it, vi } from "vitest"

describe("createCoalescedRequestAnimationFrameEmitter", () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it("emits once when schedule is called multiple times before rAF fires", () => {
    const emit = vi.fn()
    let scheduled: FrameRequestCallback | null = null
    const raf = vi.fn((cb: FrameRequestCallback) => {
      scheduled = cb
      return 1 as unknown as number
    })
    const caf = vi.fn()

    const emitter = createCoalescedRequestAnimationFrameEmitter({
      emit,
      requestAnimationFrame: raf as typeof requestAnimationFrame,
      cancelAnimationFrame: caf,
    })

    emitter.schedule()
    emitter.schedule()
    emitter.schedule()
    expect(raf).toHaveBeenCalledTimes(1)
    expect(emit).not.toHaveBeenCalled()

    scheduled!(0)
    expect(emit).toHaveBeenCalledTimes(1)

    emitter.schedule()
    expect(raf).toHaveBeenCalledTimes(2)
  })

  it("cancel drops pending emit", () => {
    const emit = vi.fn()
    let scheduled: FrameRequestCallback | null = null
    const raf = vi.fn((cb: FrameRequestCallback) => {
      scheduled = cb
      return 7 as unknown as number
    })
    const caf = vi.fn()

    const emitter = createCoalescedRequestAnimationFrameEmitter({
      emit,
      requestAnimationFrame: raf as typeof requestAnimationFrame,
      cancelAnimationFrame: caf,
    })

    emitter.schedule()
    emitter.cancel()
    expect(caf).toHaveBeenCalledWith(7)
    expect(emit).not.toHaveBeenCalled()

    emitter.schedule()
    scheduled!(0)
    expect(emit).toHaveBeenCalledTimes(1)
  })
})

describe("attachPdfBookViewerGeometryResampleListeners", () => {
  it("schedules emit on window resize, ResizeObserver, scalechanging, rotationchanging", () => {
    const scheduleEmit = vi.fn()
    const listeners: Record<string, (() => void)[]> = {}
    const win = {
      addEventListener: vi.fn((name: string, fn: () => void) => {
        listeners[name] = listeners[name] ?? []
        listeners[name].push(fn)
      }),
      removeEventListener: vi.fn((name: string, fn: () => void) => {
        const arr = listeners[name]
        if (!arr) return
        const i = arr.indexOf(fn)
        if (i >= 0) arr.splice(i, 1)
      }),
    } as unknown as Window

    const roInstances: { cb: () => void; el: Element }[] = []
    class MockRO {
      cb: () => void
      constructor(cb: () => void) {
        this.cb = cb
      }
      observe(el: Element) {
        roInstances.push({ cb: this.cb, el })
      }
      disconnect() {
        roInstances.length = 0
      }
    }

    const bus = {
      on: vi.fn((name: string, fn: () => void) => {
        const key = `bus:${name}`
        ;(listeners[key] ??= []).push(fn)
      }),
      off: vi.fn((name: string, fn: () => void) => {
        const key = `bus:${name}`
        const arr = listeners[key]
        if (!arr) return
        const i = arr.indexOf(fn)
        if (i >= 0) arr.splice(i, 1)
      }),
    }

    const container = document.createElement("div")
    const detach = attachPdfBookViewerGeometryResampleListeners({
      container,
      eventBus: bus,
      scheduleEmit,
      targetWindow: win,
      ResizeObserver: MockRO as unknown as typeof ResizeObserver,
    })

    expect(roInstances).toHaveLength(1)
    const ro0 = roInstances[0]!
    expect(ro0.el).toBe(container)

    listeners.resize![0]!()
    listeners["bus:scalechanging"]![0]!()
    listeners["bus:rotationchanging"]![0]!()
    ro0.cb()
    expect(scheduleEmit).toHaveBeenCalledTimes(4)

    detach()
    expect(win.removeEventListener).toHaveBeenCalled()
    expect(bus.off).toHaveBeenCalledWith("scalechanging", expect.any(Function))
    expect(bus.off).toHaveBeenCalledWith(
      "rotationchanging",
      expect.any(Function)
    )
  })
})

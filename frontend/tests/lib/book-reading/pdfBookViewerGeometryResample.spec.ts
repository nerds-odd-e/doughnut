import {
  attachPdfBookViewerGeometryResampleListeners,
  createCoalescedRequestAnimationFrameEmitter,
} from "@/lib/book-reading/pdfBookViewerGeometryResample"
import { afterEach, describe, expect, it, vi } from "vitest"

type ListenerMap = Record<string, (() => void)[]>

function createMockWindow(listeners: ListenerMap) {
  return {
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
}

function createMockEventBus(listeners: ListenerMap) {
  return {
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
}

function createTrackingResizeObserver(
  instances: { cb: () => void; el: Element }[]
) {
  return class MockRO {
    cb: () => void
    constructor(cb: () => void) {
      this.cb = cb
    }
    observe(el: Element) {
      instances.push({ cb: this.cb, el })
    }
    disconnect() {
      instances.length = 0
    }
  } as unknown as typeof ResizeObserver
}

function createMockRaf(rafId: number) {
  const pending = { callback: null as FrameRequestCallback | null }
  const raf = vi.fn((cb: FrameRequestCallback) => {
    pending.callback = cb
    return rafId as unknown as number
  })
  const caf = vi.fn()
  return { pending, raf, caf }
}

describe("createCoalescedRequestAnimationFrameEmitter", () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it("emits once when schedule is called multiple times before rAF fires", () => {
    const emit = vi.fn()
    const { pending, raf, caf } = createMockRaf(1)

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

    pending.callback!(0)
    expect(emit).toHaveBeenCalledTimes(1)

    emitter.schedule()
    expect(raf).toHaveBeenCalledTimes(2)
  })

  it("cancel drops pending emit", () => {
    const emit = vi.fn()
    const { pending, raf, caf } = createMockRaf(7)

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
    pending.callback!(0)
    expect(emit).toHaveBeenCalledTimes(1)
  })
})

describe("attachPdfBookViewerGeometryResampleListeners", () => {
  it("schedules emit on window resize, ResizeObserver, scalechanging, rotationchanging", () => {
    const scheduleEmit = vi.fn()
    const listeners: ListenerMap = {}
    const win = createMockWindow(listeners)
    const bus = createMockEventBus(listeners)
    const roInstances: { cb: () => void; el: Element }[] = []
    const container = document.createElement("div")

    const detach = attachPdfBookViewerGeometryResampleListeners({
      container,
      eventBus: bus,
      scheduleEmit,
      targetWindow: win,
      ResizeObserver: createTrackingResizeObserver(roInstances),
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

  it("uses scheduleEmitOnScaleChange for scalechanging when provided", () => {
    const scheduleEmit = vi.fn()
    const scheduleEmitOnScaleChange = vi.fn()
    const listeners: ListenerMap = {}
    const win = createMockWindow(listeners)
    const bus = createMockEventBus(listeners)
    const roInstances: { cb: () => void; el: Element }[] = []
    const container = document.createElement("div")

    attachPdfBookViewerGeometryResampleListeners({
      container,
      eventBus: bus,
      scheduleEmit,
      scheduleEmitOnScaleChange,
      targetWindow: win,
      ResizeObserver: createTrackingResizeObserver(roInstances),
    })

    listeners["bus:scalechanging"]![0]!()
    listeners["bus:rotationchanging"]![0]!()
    listeners.resize![0]!()

    expect(scheduleEmitOnScaleChange).toHaveBeenCalledTimes(1)
    expect(scheduleEmit).toHaveBeenCalledTimes(2)
  })
})

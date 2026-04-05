export type PdfViewerGeometryEventBus = {
  on: (name: string, fn: () => void) => void
  off: (name: string, fn: () => void) => void
}

export function createCoalescedRequestAnimationFrameEmitter(options: {
  emit: () => void
  requestAnimationFrame?: (
    callback: FrameRequestCallback
  ) => ReturnType<typeof requestAnimationFrame>
  cancelAnimationFrame?: (id: ReturnType<typeof requestAnimationFrame>) => void
}) {
  const raf = options.requestAnimationFrame ?? globalThis.requestAnimationFrame
  const caf = options.cancelAnimationFrame ?? globalThis.cancelAnimationFrame
  let id: ReturnType<typeof requestAnimationFrame> | null = null
  return {
    schedule() {
      if (id !== null) return
      id = raf(() => {
        id = null
        options.emit()
      })
    },
    cancel() {
      if (id !== null) {
        caf(id)
        id = null
      }
    },
  }
}

/**
 * Re-schedule viewport sampling after layout-affecting changes (window resize, container size,
 * pdf.js scale/rotation). Callers own coalescing (e.g. rAF) via `scheduleEmit`.
 */
export function attachPdfBookViewerGeometryResampleListeners(options: {
  container: HTMLElement
  eventBus: PdfViewerGeometryEventBus
  scheduleEmit: () => void
  targetWindow?: Window
  ResizeObserver?: typeof ResizeObserver
}): () => void {
  const win = options.targetWindow ?? window
  const RO = options.ResizeObserver ?? ResizeObserver

  const onWinResize = () => options.scheduleEmit()
  win.addEventListener("resize", onWinResize, { passive: true })

  const onScale = () => options.scheduleEmit()
  const onRotation = () => options.scheduleEmit()
  options.eventBus.on("scalechanging", onScale)
  options.eventBus.on("rotationchanging", onRotation)

  const ro = new RO(() => options.scheduleEmit())
  ro.observe(options.container)

  return () => {
    win.removeEventListener("resize", onWinResize)
    options.eventBus.off("scalechanging", onScale)
    options.eventBus.off("rotationchanging", onRotation)
    ro.disconnect()
  }
}

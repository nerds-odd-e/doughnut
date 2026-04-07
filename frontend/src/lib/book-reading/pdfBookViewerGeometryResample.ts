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
 *
 * Use `scheduleEmitOnScaleChange` when `scheduleEmit` also reapplies default zoom: pdf.js fires
 * `scalechanging` for every programmatic scale update, so the same callback would loop
 * (e.g. page-width then clamp) every frame on wide viewports.
 */
export function attachPdfBookViewerGeometryResampleListeners(options: {
  container: HTMLElement
  eventBus: PdfViewerGeometryEventBus
  scheduleEmit: () => void
  scheduleEmitOnScaleChange?: () => void
  targetWindow?: Window
  ResizeObserver?: typeof ResizeObserver
}): () => void {
  const win = options.targetWindow ?? window
  const RO = options.ResizeObserver ?? ResizeObserver
  const onScale = () =>
    (options.scheduleEmitOnScaleChange ?? options.scheduleEmit)()

  const onWinResize = () => options.scheduleEmit()
  win.addEventListener("resize", onWinResize, { passive: true })

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

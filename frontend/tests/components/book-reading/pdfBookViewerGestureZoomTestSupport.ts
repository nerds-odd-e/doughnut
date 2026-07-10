import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, vi } from "vitest"

const harness = vi.hoisted(() => {
  const mockPdf = {
    async getPage(pageNumber: number) {
      if (pageNumber !== 1) {
        throw new Error("mock pdf: page not found")
      }
      return {
        getViewport({ scale }: { scale: number }) {
          return { width: 612 * scale, height: 792 * scale }
        },
      }
    },
  }
  return {
    mockPdf,
    lastViewer: null as null | { currentScale: number },
  }
})

vi.mock("pdfjs-dist/build/pdf.mjs", () => ({
  GlobalWorkerOptions: { workerSrc: "" },
  getDocument: vi.fn(() => ({
    promise: Promise.resolve(harness.mockPdf),
    destroy: vi.fn(),
  })),
}))

vi.mock("pdfjs-dist/web/pdf_viewer.mjs", () => {
  class EventBus {
    private listeners = new Map<string, Set<(...args: unknown[]) => void>>()

    on(name: string, fn: (...args: unknown[]) => void) {
      let set = this.listeners.get(name)
      if (!set) {
        set = new Set()
        this.listeners.set(name, set)
      }
      set.add(fn)
    }

    off(name: string, fn: (...args: unknown[]) => void) {
      this.listeners.get(name)?.delete(fn)
    }

    dispatch(name: string, arg?: unknown) {
      for (const fn of this.listeners.get(name) ?? []) {
        fn(arg)
      }
    }
  }

  class PDFLinkService {
    eventBus: EventBus
    constructor(options: { eventBus: EventBus }) {
      this.eventBus = options.eventBus
    }

    setViewer() {
      return
    }

    setDocument() {
      return
    }
  }

  class PDFViewer {
    container: HTMLElement
    viewer: HTMLElement
    eventBus: EventBus
    linkService: PDFLinkService
    pagesCount = 0
    pdfDocument: unknown = null
    currentPageNumber = 1
    private pageDiv: HTMLDivElement | null = null
    _internalScale = 1

    get currentScale() {
      return this._internalScale
    }

    set currentScale(v: number) {
      this._internalScale = Math.min(25, Math.max(0.1, v))
    }

    set currentScaleValue(_v: string) {
      const w = this.container.clientWidth || 800
      this._internalScale = Math.max(0.5, w / 612)
    }

    constructor(options: {
      container: HTMLElement
      viewer: HTMLElement
      eventBus: EventBus
      linkService: PDFLinkService
    }) {
      this.container = options.container
      this.viewer = options.viewer
      this.eventBus = options.eventBus
      this.linkService = options.linkService
      harness.lastViewer = this
    }

    setDocument(doc: unknown) {
      this.pdfDocument = doc
      this.viewer.innerHTML = ""
      if (doc) {
        this.pagesCount = 1
        const div = document.createElement("div")
        div.style.width = "612px"
        div.style.height = "792px"
        this.viewer.appendChild(div)
        this.pageDiv = div
        queueMicrotask(() => {
          this.eventBus.dispatch("pagesinit", {})
        })
      } else {
        this.pagesCount = 0
        this.pageDiv = null
      }
    }

    getPageView(index: number) {
      if (index !== 0 || !this.pageDiv) return
      return {
        div: this.pageDiv,
        viewport: { width: 612, height: 792 },
      }
    }

    scrollPageIntoView() {
      return
    }
  }

  return { EventBus, PDFLinkService, PDFViewer }
})

export function pdfViewerContainerEl(host: HTMLElement) {
  return host.querySelector('[data-testid="pdf-book-viewer"]') as HTMLElement
}

export async function flushAnimationFrame() {
  await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
}

export function resetPdfGestureZoomHarness() {
  harness.lastViewer = null
}

export function setupPdfBookViewerGestureZoomTests() {
  beforeEach(() => {
    resetPdfGestureZoomHarness()
  })
}

export async function mountGestureZoomViewerReady() {
  const wrapper = helper
    .component(PdfBookViewer)
    .withProps({ pdfBytes: new Uint8Array([1, 2, 3]) })
    .mount()

  const host = wrapper.element as HTMLElement
  host.style.position = "relative"
  host.style.width = "400px"
  host.style.height = "500px"

  await flushPromises()
  await flushAnimationFrame()

  const container = pdfViewerContainerEl(host)
  const viewer = harness.lastViewer

  return { wrapper, host, container, viewer }
}

export function dispatchWheelOnViewer(
  container: HTMLElement,
  host: HTMLElement,
  options: { ctrlKey: boolean; metaKey: boolean; deltaY?: number }
) {
  const hostRect = host.getBoundingClientRect()
  const ev = new WheelEvent("wheel", {
    deltaY: options.deltaY ?? -80,
    ctrlKey: options.ctrlKey,
    metaKey: options.metaKey,
    clientX: hostRect.left + 80,
    clientY: hostRect.top + 120,
    bubbles: true,
    cancelable: true,
  })
  const notCanceled = container.dispatchEvent(ev)
  return { ev, notCanceled }
}

export function dispatchPinchZoom(container: HTMLElement) {
  const r = container.getBoundingClientRect()
  const mkTouch = (id: number, x: number, y: number) =>
    new Touch({
      identifier: id,
      target: container,
      clientX: r.left + x,
      clientY: r.top + y,
      radiusX: 1,
      radiusY: 1,
      rotationAngle: 0,
      force: 0.5,
    })

  const t0a = mkTouch(0, 100, 200)
  const t1a = mkTouch(1, 200, 200)
  container.dispatchEvent(
    new TouchEvent("touchstart", {
      bubbles: true,
      cancelable: true,
      touches: [t0a, t1a],
      targetTouches: [t0a, t1a],
      changedTouches: [t1a],
    })
  )

  const t0b = mkTouch(0, 100, 200)
  const t1b = mkTouch(1, 300, 200)
  container.dispatchEvent(
    new TouchEvent("touchmove", {
      bubbles: true,
      cancelable: true,
      touches: [t0b, t1b],
      targetTouches: [t0b, t1b],
      changedTouches: [t1b],
    })
  )
}

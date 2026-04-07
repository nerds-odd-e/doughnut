import PdfBookViewer from "@/components/book-reading/PdfBookViewer.vue"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"

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

vi.mock("pdfjs-dist/legacy/build/pdf.mjs", () => ({
  GlobalWorkerOptions: { workerSrc: "" },
  getDocument: vi.fn(() => ({
    promise: Promise.resolve(harness.mockPdf),
    destroy: vi.fn(),
  })),
}))

vi.mock("pdfjs-dist/legacy/web/pdf_viewer.mjs", () => {
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

async function flushRafs() {
  await new Promise<void>((r) => requestAnimationFrame(() => r()))
}

function mountViewerHost() {
  const wrapper = helper
    .component(PdfBookViewer)
    .withProps({ pdfBytes: new Uint8Array([1, 2, 3]) })
    .mount()

  const host = wrapper.element as HTMLElement
  host.style.position = "relative"
  host.style.width = "400px"
  host.style.height = "500px"

  return { wrapper, host }
}

describe("PdfBookViewer gesture zoom (mocked pdf.js)", () => {
  beforeEach(() => {
    harness.lastViewer = null
  })

  it("ctrl+wheel on the viewer prevents default and updates pdf scale", async () => {
    const { wrapper, host } = mountViewerHost()
    await flushPromises()
    await flushRafs()

    const viewer = harness.lastViewer
    expect(viewer).not.toBeNull()
    const baseline = viewer!.currentScale

    const container = wrapper.find('[data-testid="pdf-book-viewer"]').element
    const hostRect = host.getBoundingClientRect()
    const ev = new WheelEvent("wheel", {
      deltaY: -80,
      ctrlKey: true,
      clientX: hostRect.left + 80,
      clientY: hostRect.top + 120,
      bubbles: true,
      cancelable: true,
    })
    const notCanceled = container.dispatchEvent(ev)

    expect(ev.defaultPrevented).toBe(true)
    expect(notCanceled).toBe(false)
    await flushRafs()
    expect(viewer!.currentScale).toBeGreaterThan(baseline)

    wrapper.unmount()
  })

  it("meta+wheel on the viewer prevents default and updates pdf scale", async () => {
    const { wrapper, host } = mountViewerHost()
    await flushPromises()
    await flushRafs()

    const viewer = harness.lastViewer
    expect(viewer).not.toBeNull()
    const baseline = viewer!.currentScale

    const container = wrapper.find('[data-testid="pdf-book-viewer"]').element
    const hostRect = host.getBoundingClientRect()
    const ev = new WheelEvent("wheel", {
      deltaY: -80,
      ctrlKey: false,
      metaKey: true,
      clientX: hostRect.left + 80,
      clientY: hostRect.top + 120,
      bubbles: true,
      cancelable: true,
    })
    container.dispatchEvent(ev)

    expect(ev.defaultPrevented).toBe(true)
    await flushRafs()
    expect(viewer!.currentScale).toBeGreaterThan(baseline)

    wrapper.unmount()
  })

  it("wheel without ctrl/meta does not cancel (no browser-zoom block path)", async () => {
    const { wrapper, host } = mountViewerHost()
    await flushPromises()
    await flushRafs()

    const viewer = harness.lastViewer
    const baseline = viewer!.currentScale

    const container = wrapper.find('[data-testid="pdf-book-viewer"]').element
    const hostRect = host.getBoundingClientRect()
    const ev = new WheelEvent("wheel", {
      deltaY: -80,
      ctrlKey: false,
      metaKey: false,
      clientX: hostRect.left + 80,
      clientY: hostRect.top + 120,
      bubbles: true,
      cancelable: true,
    })
    container.dispatchEvent(ev)

    expect(ev.defaultPrevented).toBe(false)
    expect(viewer!.currentScale).toBe(baseline)

    wrapper.unmount()
  })

  it("two-finger pinch touchmove updates scale around the midpoint", async () => {
    const { wrapper } = mountViewerHost()
    await flushPromises()
    await flushRafs()

    const viewer = harness.lastViewer
    const baseline = viewer!.currentScale

    const container = wrapper.find('[data-testid="pdf-book-viewer"]').element
    const r = container.getBoundingClientRect()

    const touchTarget = container
    const mkTouch = (id: number, x: number, y: number) =>
      new Touch({
        identifier: id,
        target: touchTarget,
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

    await flushRafs()
    expect(viewer!.currentScale).toBeGreaterThan(baseline)

    wrapper.unmount()
  })
})

import { epubRenditionResizeDimensions } from "@/lib/book-reading/epubRenditionHostSize"
import { describe, expect, it } from "vitest"

describe("epubRenditionResizeDimensions", () => {
  it("uses the host element size, not a narrower inner .epub-container (scrollbar gutter)", () => {
    const host = document.createElement("div")
    Object.defineProperty(host, "clientWidth", {
      configurable: true,
      value: 700,
    })
    Object.defineProperty(host, "clientHeight", {
      configurable: true,
      value: 900,
    })
    const inner = document.createElement("div")
    inner.className = "epub-container"
    Object.defineProperty(inner, "clientWidth", {
      configurable: true,
      value: 683,
    })
    Object.defineProperty(inner, "clientHeight", {
      configurable: true,
      value: 900,
    })
    host.appendChild(inner)

    expect(epubRenditionResizeDimensions(host)).toEqual({
      width: 700,
      height: 900,
    })
  })

  it("returns null when host has no usable size", () => {
    const host = document.createElement("div")
    Object.defineProperty(host, "clientWidth", {
      configurable: true,
      value: 0,
    })
    Object.defineProperty(host, "clientHeight", {
      configurable: true,
      value: 100,
    })
    expect(epubRenditionResizeDimensions(host)).toBeNull()
  })

  it("floors fractional layout pixels", () => {
    const host = document.createElement("div")
    Object.defineProperty(host, "clientWidth", {
      configurable: true,
      value: 400.9,
    })
    Object.defineProperty(host, "clientHeight", {
      configurable: true,
      value: 600.2,
    })
    expect(epubRenditionResizeDimensions(host)).toEqual({
      width: 400,
      height: 600,
    })
  })
})

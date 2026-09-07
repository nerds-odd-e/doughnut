import { routeMetadata } from "@/routes/routeMetadata"
import recoveryDocument from "../../../backend/src/main/resources/bad-request-recovery.html?raw"
import { afterEach, describe, expect, it } from "vitest"

let frame: HTMLIFrameElement

afterEach(() => frame?.remove())

const loaded = (element: HTMLIFrameElement) =>
  new Promise<void>((resolve) =>
    element.addEventListener("load", () => resolve(), { once: true })
  )

describe("bad request recovery on a tablet", () => {
  it.each([
    [768, 1024],
    [1024, 768],
  ])(
    "keeps instructions readable and the homepage usable at %ix%i",
    async (width, height) => {
      frame = document.createElement("iframe")
      frame.setAttribute("sandbox", "allow-same-origin")
      frame.style.cssText = `width: ${width}px; height: ${height}px; border: 0`
      frame.srcdoc = recoveryDocument.replace(
        "<head>",
        `<head><meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'">`
      )
      const ready = loaded(frame)
      document.body.append(frame)
      await ready
      const content = frame.contentDocument!
      const view = frame.contentWindow!

      expect(
        content.querySelector('meta[name="viewport"]')?.getAttribute("content")
      ).toContain("width=device-width")
      expect(content.documentElement.scrollWidth).toBeLessThanOrEqual(width)
      expect(content.body.innerText).toContain("Private tab")
      expect(content.body.innerText).toContain("local search history")
      for (const instruction of content.querySelectorAll("p, li")) {
        const style = view.getComputedStyle(instruction)
        const fontSize = Number.parseFloat(style.fontSize)
        expect(fontSize).toBeGreaterThanOrEqual(18)
        expect(Number.parseFloat(style.lineHeight)).toBeGreaterThanOrEqual(
          fontSize * 1.5
        )
        const bounds = instruction.getBoundingClientRect()
        expect(bounds.width).toBeGreaterThan(0)
        expect(bounds.left).toBeGreaterThanOrEqual(0)
        expect(bounds.right).toBeLessThanOrEqual(width)
      }

      const homepage = content.querySelector("a")!
      const homePath = routeMetadata.find(({ name }) => name === "root")!.path
      expect(homepage.getAttribute("href")).toBe(homePath)
      expect(homepage.getBoundingClientRect().height).toBeGreaterThanOrEqual(44)
      const originalLocation = window.location.href
      const navigated = loaded(frame)
      homepage.click()
      await navigated
      expect(view.location.pathname).toBe(homePath)
      expect(view.location.search).toBe("")
      expect(window.location.href).toBe(originalLocation)
    }
  )
})

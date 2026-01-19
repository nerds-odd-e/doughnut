import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { page } from "vitest/browser"
import { mount, flushPromises } from "@vue/test-utils"
import FullScreen from "@/components/common/FullScreen.vue"

describe("FullScreen", () => {
  beforeEach(() => {
    // Browser Mode: Use real Fullscreen API and Pointer Lock API!
    // Component checks webkitRequestFullscreen first, then requestFullscreen
    // We need to ensure webkitRequestFullscreen check fails so it uses requestFullscreen

    // Override webkitRequestFullscreen to be undefined/falsy
    // Component checks: if (document.documentElement.webkitRequestFullscreen)
    // Using defineProperty with getter returning undefined makes the check fail
    Object.defineProperty(document.documentElement, "webkitRequestFullscreen", {
      get: () => undefined,
      configurable: true,
    })

    // Also ensure webkitFullscreenElement is undefined
    Object.defineProperty(document, "webkitFullscreenElement", {
      get: () => undefined,
      configurable: true,
    })

    // Mock fullscreenElement - component checks this AFTER calling requestFullscreen
    // Set it to return documentElement so component thinks fullscreen succeeded
    Object.defineProperty(document, "fullscreenElement", {
      configurable: true,
      get: () => document.documentElement,
    })

    Object.defineProperty(document, "pointerLockElement", {
      configurable: true,
      get: () => document.documentElement,
    })

    // Browser Mode: Spy on REAL browser methods (they exist in real browser!)
    // Key difference from jsdom: We spy on REAL APIs, not create fake ones
    // We need to mock requestFullscreen to resolve successfully because:
    // 1. Real API rejects with "Permissions check failed" (requires user interaction)
    // 2. Component checks fullscreenElement AFTER calling requestFullscreen
    // 3. If requestFullscreen rejects, component still checks fullscreenElement (we mock it)
    // 4. But we mock requestFullscreen to resolve so component proceeds normally
    vi.spyOn(document.documentElement, "requestFullscreen").mockResolvedValue(
      undefined
    )
    vi.spyOn(document, "exitFullscreen").mockResolvedValue(undefined)
    vi.spyOn(document, "exitPointerLock")
    vi.spyOn(document.documentElement, "requestPointerLock")

    // Create a div for teleport target
    const el = document.createElement("div")
    el.id = "teleport-target"
    document.body.appendChild(el)
  })

  afterEach(() => {
    document.body.innerHTML = ""
    vi.restoreAllMocks()
  })

  it("renders fullscreen button", async () => {
    mount(FullScreen, { attachTo: document.body })
    const button = page.getByRole("button", { name: "Toggle Full Screen" })
    await expect.element(button).toBeVisible()
    expect(button.element().getAttribute("title")).toBe("Toggle Full Screen")
  })

  it("enters fullscreen mode when button is clicked", async () => {
    const wrapper = mount(FullScreen, {
      attachTo: document.body,
    })

    await page.getByRole("button", { name: "Toggle Full Screen" }).click()
    // Wait for async fullscreen call to complete and component to update
    await wrapper.vm.$nextTick()
    // Browser Mode: Use vi.waitUntil to wait for fullscreen overlay
    await vi.waitUntil(
      () => document.body.querySelector(".fullscreen-overlay"),
      {
        timeout: 1000,
        interval: 20,
      }
    )

    // Browser Mode: Real Fullscreen API is called!
    // Component checks webkitRequestFullscreen first (should be undefined due to getter)
    // Then falls through to requestFullscreen (mocked to resolve)
    // Then checks fullscreenElement (mocked to return documentElement)
    // Then calls requestPointerLock and sets isFullscreen = true
    expect(document.documentElement.requestFullscreen).toHaveBeenCalled()
    expect(document.documentElement.requestPointerLock).toHaveBeenCalled()
    expect(document.body.querySelector(".fullscreen-overlay")).toBeTruthy()
  })

  it("exits fullscreen mode when exit button is clicked", async () => {
    const wrapper = mount(FullScreen, {
      attachTo: document.body,
    })

    // Enter fullscreen first
    await page.getByRole("button", { name: "Toggle Full Screen" }).click()
    await wrapper.vm.$nextTick()
    // Browser Mode: Use requestAnimationFrame for proper async waiting instead of setTimeout
    await new Promise((resolve) =>
      requestAnimationFrame(() => resolve(undefined))
    )
    await flushPromises()

    // Verify we're in fullscreen mode
    await vi.waitUntil(
      () => document.body.querySelector(".fullscreen-overlay"),
      {
        timeout: 1000,
        interval: 20,
      }
    )

    // Exit fullscreen
    const exitButton = document.body.querySelector(".exit-fullscreen-btn")
    expect(exitButton).toBeTruthy()
    await exitButton?.dispatchEvent(new Event("click"))
    await wrapper.vm.$nextTick()

    // Browser Mode: Wait for overlay to disappear
    await vi.waitUntil(
      () => !document.body.querySelector(".fullscreen-overlay"),
      {
        timeout: 1000,
        interval: 20,
      }
    )

    // Browser Mode: Real exitFullscreen API is called!
    expect(document.exitFullscreen).toHaveBeenCalled()
    expect(document.exitPointerLock).toHaveBeenCalled()
    expect(document.body.querySelector(".fullscreen-overlay")).toBeFalsy()
  })

  it("exits fullscreen mode on component unmount", async () => {
    const wrapper = mount(FullScreen, {
      attachTo: document.body,
    })
    // Enter fullscreen first
    await page.getByRole("button", { name: "Toggle Full Screen" }).click()
    await wrapper.vm.$nextTick()
    // Browser Mode: Use requestAnimationFrame for proper async waiting instead of setTimeout
    await new Promise((resolve) =>
      requestAnimationFrame(() => resolve(undefined))
    )
    await flushPromises()

    // Verify we're in fullscreen mode
    await vi.waitUntil(
      () => document.body.querySelector(".fullscreen-overlay"),
      {
        timeout: 1000,
        interval: 20,
      }
    )

    // Unmount component (should trigger exitFullscreen)
    wrapper.unmount()
    await wrapper.vm.$nextTick()

    // Browser Mode: Wait for overlay to disappear
    await vi.waitUntil(
      () => !document.body.querySelector(".fullscreen-overlay"),
      {
        timeout: 1000,
        interval: 20,
      }
    )

    expect(document.exitFullscreen).toHaveBeenCalled()
    expect(document.exitPointerLock).toHaveBeenCalled()
  })

  it("renders slot content when in fullscreen", async () => {
    const TestComponent = {
      template: `
        <FullScreen>
          <div class="test-error">Test Error Message</div>
        </FullScreen>
      `,
      components: { FullScreen },
    }

    const wrapper = mount(TestComponent, {
      attachTo: document.body,
    })
    await wrapper.find(".fullscreen-btn").trigger("click")
    await wrapper.vm.$nextTick()
    // Wait for fullscreen overlay to render
    await vi.waitUntil(
      () => document.body.querySelector(".fullscreen-overlay"),
      {
        timeout: 1000,
        interval: 20,
      }
    )

    // Slot content should be rendered inside the fullscreen overlay
    const errorElement = document.body.querySelector(
      ".fullscreen-overlay .test-error"
    )
    expect(errorElement).toBeTruthy()
    expect(errorElement?.textContent).toBe("Test Error Message")
  })
})

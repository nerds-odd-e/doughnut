import FullScreen from "@/components/common/FullScreen.vue"
import { flushPromises, mount, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

const TOGGLE_FULL_SCREEN_TITLE = "Toggle Full Screen"
const SLOT_CONTENT = "Test Error Message"

describe("FullScreen", () => {
  let wrapper: VueWrapper | undefined

  beforeEach(() => {
    let currentFullscreenElement: Element | null = null

    Object.defineProperty(document.documentElement, "webkitRequestFullscreen", {
      get: () => undefined,
      configurable: true,
    })
    Object.defineProperty(document, "webkitFullscreenElement", {
      get: () => undefined,
      configurable: true,
    })
    Object.defineProperty(document, "fullscreenElement", {
      configurable: true,
      get: () => currentFullscreenElement,
    })
    Object.defineProperty(document, "pointerLockElement", {
      configurable: true,
      get: () => currentFullscreenElement,
    })

    vi.spyOn(document.documentElement, "requestFullscreen").mockImplementation(
      async function (this: Element) {
        currentFullscreenElement = this
      }
    )
    vi.spyOn(document, "exitFullscreen").mockImplementation(async () => {
      // Keep pointerLockElement mock state so exitPointerLock runs in component logic.
    })
    vi.spyOn(document, "exitPointerLock")
    vi.spyOn(document.documentElement, "requestPointerLock")
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
    document.body.innerHTML = ""
    vi.restoreAllMocks()
  })

  const mountFullScreen = (slot?: string) => {
    wrapper = mount(FullScreen, {
      attachTo: document.body,
      slots: slot ? { default: slot } : undefined,
    })
    return wrapper
  }

  const fullscreenOverlay = () =>
    document.body.querySelector(".fullscreen-overlay")

  const clickToggleFullScreen = async (w: VueWrapper) => {
    await w.find(".fullscreen-btn").trigger("click")
    await flushPromises()
  }

  const enterFullscreen = async (w: VueWrapper) => {
    await clickToggleFullScreen(w)
    expect(fullscreenOverlay()).toBeTruthy()
  }

  const clickExitFullScreen = async () => {
    const exitButton = document.body.querySelector(".exit-fullscreen-btn")
    expect(exitButton).toBeTruthy()
    ;(exitButton as HTMLButtonElement).click()
    await flushPromises()
  }

  it("enters fullscreen mode when button is clicked", async () => {
    const w = mountFullScreen(`<div class="test-error">${SLOT_CONTENT}</div>`)
    expect(w.find(".fullscreen-btn").attributes("title")).toBe(
      TOGGLE_FULL_SCREEN_TITLE
    )

    await enterFullscreen(w)

    expect(document.documentElement.requestFullscreen).toHaveBeenCalled()
    expect(document.documentElement.requestPointerLock).toHaveBeenCalled()
    expect(fullscreenOverlay()?.querySelector(".test-error")?.textContent).toBe(
      SLOT_CONTENT
    )
  })

  it("exits fullscreen mode when exit button is clicked", async () => {
    const w = mountFullScreen()
    await enterFullscreen(w)

    await clickExitFullScreen()

    expect(document.exitFullscreen).toHaveBeenCalled()
    expect(document.exitPointerLock).toHaveBeenCalled()
    expect(fullscreenOverlay()).toBeFalsy()
  })

  it("exits fullscreen mode on component unmount", async () => {
    const w = mountFullScreen()
    await enterFullscreen(w)

    const unmountingWrapper = w
    wrapper = undefined
    unmountingWrapper.unmount()
    await flushPromises()

    expect(document.exitFullscreen).toHaveBeenCalled()
    expect(document.exitPointerLock).toHaveBeenCalled()
    expect(fullscreenOverlay()).toBeFalsy()
  })
})

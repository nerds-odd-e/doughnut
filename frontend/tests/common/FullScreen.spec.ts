import FullScreen from "@/components/common/FullScreen.vue"
import { flushPromises, mount, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { page } from "vitest/browser"

const TOGGLE_FULL_SCREEN_TITLE = "Toggle Full Screen"

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

  const mountFullScreen = () => {
    wrapper = mount(FullScreen, { attachTo: document.body })
    return wrapper
  }

  const fullscreenOverlay = () =>
    document.body.querySelector(".fullscreen-overlay")

  const clickToggleFullScreen = async () => {
    const button = page.getByTitle(TOGGLE_FULL_SCREEN_TITLE)
    await expect.element(button).toBeVisible()
    await button.click()
    await flushPromises()
  }

  const enterFullscreen = async (w: VueWrapper) => {
    await clickToggleFullScreen()
    await w.vm.$nextTick()
    await flushPromises()
    expect(fullscreenOverlay()).toBeTruthy()
  }

  it("enters fullscreen mode when button is clicked", async () => {
    const w = mountFullScreen()
    expect(w.find(".fullscreen-btn").attributes("title")).toBe(
      TOGGLE_FULL_SCREEN_TITLE
    )

    await clickToggleFullScreen()

    expect(document.documentElement.requestFullscreen).toHaveBeenCalled()
    expect(document.documentElement.requestPointerLock).toHaveBeenCalled()
    expect(fullscreenOverlay()).toBeTruthy()
  })

  it("exits fullscreen mode when exit button is clicked", async () => {
    const w = mountFullScreen()
    await enterFullscreen(w)

    await page.getByText("Exit Full Screen").click()
    await flushPromises()

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

  it("renders slot content when in fullscreen", async () => {
    const TestComponent = {
      template: `
        <FullScreen>
          <div class="test-error">Test Error Message</div>
        </FullScreen>
      `,
      components: { FullScreen },
    }

    wrapper = mount(TestComponent, { attachTo: document.body })
    await enterFullscreen(wrapper)

    const errorElement = document.body.querySelector(
      ".fullscreen-overlay .test-error"
    )
    expect(errorElement?.textContent).toBe("Test Error Message")
  })
})

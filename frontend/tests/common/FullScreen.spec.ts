import { mount } from "@vue/test-utils"
import { describe, it, expect, beforeEach, vi } from "vitest"
import FullScreen from "@/components/common/FullScreen.vue"

describe("FullScreen", () => {
  beforeEach(() => {
    // Mock document fullscreen methods
    document.documentElement.requestFullscreen = vi
      .fn()
      .mockResolvedValue(undefined)
    document.exitFullscreen = vi.fn().mockResolvedValue(undefined)
    document.exitPointerLock = vi.fn()
    document.documentElement.requestPointerLock = vi.fn()

    // Mock fullscreenElement and pointerLockElement getters
    Object.defineProperty(document, "fullscreenElement", {
      configurable: true,
      get: () => document.documentElement,
    })

    Object.defineProperty(document, "pointerLockElement", {
      configurable: true,
      get: () => document.documentElement,
    })

    // Create a div for teleport target
    const el = document.createElement("div")
    el.id = "teleport-target"
    document.body.appendChild(el)
  })

  afterEach(() => {
    document.body.innerHTML = ""
  })

  it("renders fullscreen button", () => {
    const wrapper = mount(FullScreen)
    const button = wrapper.find(".fullscreen-btn")
    expect(button.exists()).toBe(true)
    expect(button.attributes("title")).toBe("Toggle Full Screen")
  })

  it("enters fullscreen mode when button is clicked", async () => {
    const wrapper = mount(FullScreen, {
      attachTo: document.body,
    })
    await wrapper.find(".fullscreen-btn").trigger("click")
    await wrapper.vm.$nextTick()

    expect(document.documentElement.requestFullscreen).toHaveBeenCalled()
    expect(document.documentElement.requestPointerLock).toHaveBeenCalled()
    expect(document.body.querySelector(".fullscreen-overlay")).toBeTruthy()
  })

  it("exits fullscreen mode when exit button is clicked", async () => {
    const wrapper = mount(FullScreen, {
      attachTo: document.body,
    })

    await wrapper.find(".fullscreen-btn").trigger("click")
    await wrapper.vm.$nextTick()

    const exitButton = document.body.querySelector(".exit-fullscreen-btn")
    await exitButton?.dispatchEvent(new Event("click"))
    await wrapper.vm.$nextTick()

    expect(document.exitFullscreen).toHaveBeenCalled()
    expect(document.exitPointerLock).toHaveBeenCalled()
    expect(document.body.querySelector(".fullscreen-overlay")).toBeFalsy()
  })

  it("exits fullscreen mode on component unmount", async () => {
    const wrapper = mount(FullScreen, {
      attachTo: document.body,
    })
    await wrapper.find(".fullscreen-btn").trigger("click")
    await wrapper.vm.$nextTick()

    wrapper.unmount()
    await wrapper.vm.$nextTick()

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

    const errorElement = document.body.querySelector(".test-error")
    expect(errorElement).toBeTruthy()
    expect(errorElement?.textContent).toBe("Test Error Message")
  })
})

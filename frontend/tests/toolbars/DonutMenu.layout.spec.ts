import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, onTestFinished } from "vitest"
import { page, server } from "vitest/browser"
import { defineComponent, h, provide, ref } from "vue"
import {
  donutMenuPanel,
  launcher,
  pressLauncher,
  router,
  setupDonutMenuTests,
  user,
} from "./donutMenuTestSupport"

async function openDonutMenuInPageBarAt(width: number) {
  const original = server.config.browser.viewport
  onTestFinished(() => page.viewport(original.width, original.height))
  await page.viewport(width, 800)
  const PageBar = defineComponent({
    setup() {
      provide("currentUser", ref(user))
      return () => h(GlobalBar)
    },
  })
  helper.component(PageBar).withRealRouter(router).render()
  await flushPromises()
  await pressLauncher()
  return donutMenuPanel().getBoundingClientRect()
}

describe("Donut menu panel layout", () => {
  setupDonutMenuTests()

  it("opens as a full-width sheet below the page bar on a phone", async () => {
    const sheet = await openDonutMenuInPageBarAt(390)
    const bar = launcher().closest("nav")!.getBoundingClientRect()

    expect(sheet.top).toBeGreaterThanOrEqual(bar.bottom)
    expect(sheet.left).toBeLessThanOrEqual(1)
    expect(sheet.right).toBeGreaterThanOrEqual(window.innerWidth - 1)
  })

  it("opens anchored below its launcher on a wide window", async () => {
    const dropdown = await openDonutMenuInPageBarAt(1200)
    const button = launcher().getBoundingClientRect()

    expect(dropdown.top).toBeGreaterThanOrEqual(button.bottom)
    expect(Math.abs(dropdown.left - button.left)).toBeLessThanOrEqual(4)
    expect(dropdown.width).toBeLessThan(window.innerWidth)
  })
})

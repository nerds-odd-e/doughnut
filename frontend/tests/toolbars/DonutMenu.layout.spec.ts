import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import { focusDirective } from "@/utils/focusTarget"
import helper from "@tests/helpers"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, onTestFinished } from "vitest"
import { page, server } from "vitest/browser"
import { defineComponent, h, provide, ref } from "vue"
import {
  donutMenuPanel,
  launcher,
  pressLauncher,
  router,
  searchLauncher,
  setupDonutMenuTests,
  user,
} from "./donutMenuTestSupport"

async function openDonutMenuInPageBarAt(
  width: number,
  { height = 800, menuUser = user } = {}
) {
  const original = server.config.browser.viewport
  onTestFinished(() => page.viewport(original.width, original.height))
  await page.viewport(width, height)
  const PageBar = defineComponent({
    setup() {
      provide("currentUser", ref(menuUser))
      return () => h(GlobalBar)
    },
  })
  helper
    .component(PageBar)
    .withDirective("focus", focusDirective)
    .withRealRouter(router)
    .render()
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

  it.each([390, 1200])(
    "keeps the account actions inside the panel at %ipx",
    async (width) => {
      await openDonutMenuInPageBarAt(width, {
        menuUser: makeMe.aUser.admin(true).please(),
      })
      await fireEvent.click(screen.getByLabelText("Account"))
      await flushPromises()
      const panel = donutMenuPanel().getBoundingClientRect()

      for (const action of [
        screen.getByText("Admin Dashboard"),
        screen.getByText("Recent..."),
        screen.getByText(/^Settings for /),
        screen.getByText("Manage Access Tokens"),
        screen.getByTitle("Logout"),
      ]) {
        const box = action.getBoundingClientRect()
        expect(box.top).toBeGreaterThanOrEqual(panel.top)
        expect(box.bottom).toBeLessThanOrEqual(panel.bottom)
        expect(box.left).toBeGreaterThanOrEqual(panel.left)
        expect(box.right).toBeLessThanOrEqual(panel.right)
      }
    }
  )

  it("fits a short window and scrolls, keeping the search launcher reachable", async () => {
    await openDonutMenuInPageBarAt(390, { height: 260 })
    await fireEvent.click(screen.getByLabelText("Account"))
    await flushPromises()

    const sheet = donutMenuPanel()
    expect(sheet.getBoundingClientRect().bottom).toBeLessThanOrEqual(
      window.innerHeight
    )
    expect(sheet.scrollHeight).toBeGreaterThan(sheet.clientHeight)

    searchLauncher().scrollIntoView()
    const field = searchLauncher().getBoundingClientRect()
    const box = sheet.getBoundingClientRect()
    expect(field.top).toBeGreaterThanOrEqual(box.top)
    expect(field.bottom).toBeLessThanOrEqual(box.bottom)
  })
})

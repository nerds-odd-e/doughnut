import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import { focusDirective } from "@/utils/focusTarget"
import helper from "@tests/helpers"
import { fireEvent, screen } from "@testing-library/vue"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { describe, expect, it, onTestFinished } from "vitest"
import { page, server, userEvent } from "vitest/browser"
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

  it.each([320, 390, 768, 1280])(
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

  it.each([320, 390, 768, 1280])(
    "spaces navigation and separates the footer at %ipx",
    async (width) => {
      await openDonutMenuInPageBarAt(width)
      const panel = donutMenuPanel()
      const box = panel.getBoundingClientRect()
      const items = Array.from(panel.querySelectorAll<HTMLElement>(".nav-item"))
      const first = items[0]!.getBoundingClientRect()
      const nextRow = items[4]!.getBoundingClientRect()
      expect(first.left - box.left).toBeGreaterThanOrEqual(12)
      expect(first.top - box.top).toBeGreaterThanOrEqual(12)
      expect(nextRow.top - first.bottom).toBeGreaterThanOrEqual(12)
      for (const item of items) {
        const label = item.querySelector(".label")!
        const range = document.createRange()
        range.selectNodeContents(label)
        const text = range.getBoundingClientRect()
        const cell = item.getBoundingClientRect()
        expect(text.left).toBeGreaterThanOrEqual(cell.left)
        expect(text.right).toBeLessThanOrEqual(cell.right)
      }
      const footer = screen.getByText("Donut by").getBoundingClientRect()
      expect(
        footer.top - searchLauncher().getBoundingClientRect().bottom
      ).toBeGreaterThanOrEqual(24)
    }
  )

  it.each([320, 390, 768, 1280])(
    "fits a short window and reaches expanded commands and footer by keyboard at %ipx",
    async (width) => {
      await openDonutMenuInPageBarAt(width, {
        height: 260,
        menuUser: makeMe.aUser.admin(true).please(),
      })
      await fireEvent.click(screen.getByLabelText("Account"))
      await flushPromises()

      const sheet = donutMenuPanel()
      expect(sheet.getBoundingClientRect().bottom).toBeLessThanOrEqual(
        window.innerHeight
      )
      expect(sheet.scrollHeight).toBeGreaterThan(sheet.clientHeight)

      screen.getByLabelText("Account").focus()
      const destinations = [
        screen.getByText("Admin Dashboard"),
        screen.getByText("Recent...").closest("a")!,
        screen.getByText(/^Settings for /),
        screen.getByText("Manage Access Tokens"),
        screen.getByTitle("Logout"),
        searchLauncher(),
        screen.getByLabelText("Search"),
        screen.getByText("Donut by"),
        sheet.querySelector<HTMLAnchorElement>('a[href="https://odd-e.com"]')!,
      ]
      for (const target of destinations) {
        await userEvent.tab()
        expect(document.activeElement).toBe(target)
        const field = target.getBoundingClientRect()
        const box = sheet.getBoundingClientRect()
        expect(field.top).toBeGreaterThanOrEqual(box.top)
        expect(field.bottom).toBeLessThanOrEqual(box.bottom)
        expect(field.left).toBeGreaterThanOrEqual(box.left)
        expect(field.right).toBeLessThanOrEqual(box.right)
      }
    }
  )
})

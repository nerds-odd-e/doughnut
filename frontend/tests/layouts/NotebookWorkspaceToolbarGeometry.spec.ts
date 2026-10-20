import { useAssimilationView } from "@/composables/useAssimilationView"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, expect, it, vi } from "vitest"
import { page } from "vitest/browser"
import {
  atViewportWidth,
  mountNotebookWorkspace,
  pageBarEl,
  pageBarCommand,
  visibleCommandTitles,
  commandRightGap,
  chooseFromMore,
  commandsSitInPageBar,
  toggleWorkspaceContents,
} from "./notebookWorkspaceHeaderTestSupport"

let wrapper: VueWrapper
afterEach(() => {
  wrapper.unmount()
  useAssimilationView().dismiss()
  document.body.innerHTML = ""
})
it.each([768, 1024, 1280])(
  "aligns contents and note tools with the pane boundary at %ipx",
  async (width) => {
    await atViewportWidth(width)
    ;({ wrapper } = await mountNotebookWorkspace([makeMe.aNoteRealm.please()]))
    await vi.waitFor(() =>
      expect(visibleCommandTitles().length).toBeGreaterThan(0)
    )
    const contents = document.querySelector<HTMLElement>(
      "[data-note-sidebar-toolbar]"
    )!
    const sidebar = document.querySelector("aside")!.getBoundingClientRect()
    const group = document
      .querySelector(".docked-contents")!
      .getBoundingClientRect()
    expect(group.right).toBe(sidebar.right)
    expect(contents.getBoundingClientRect().right).toBeLessThanOrEqual(
      sidebar.right
    )
    const create = contents
      .querySelector('[title="New folder"]')!
      .getBoundingClientRect()
    const command = pageBarCommand(
      visibleCommandTitles()[0]!
    )!.getBoundingClientRect()
    expect(create.top + create.height / 2).toBe(
      command.top + command.height / 2
    )
    expect(pageBarEl().contains(contents)).toBe(true)
    expect(Math.abs(commandRightGap())).toBeLessThanOrEqual(1)
    await page
      .elementLocator(contents.querySelector('[title="Sort sidebar"]')!)
      .click()
    await flushPromises()
    const menu = document.querySelector<HTMLElement>(
      "[data-dropdown-portal-panel]"
    )!
    await vi.waitFor(() => {
      const bounds = menu.getBoundingClientRect()
      expect(bounds.left).toBeGreaterThanOrEqual(0)
      expect(bounds.right).toBeLessThanOrEqual(width)
      expect(menu.checkVisibility()).toBe(true)
    })
    await page.screenshot({
      path: `../../vitest-test-results/sidebar-tools-${width}.png`,
    })
  }
)

it("keeps the active note action through contents toggles and narrowing", async () => {
  await atViewportWidth(320)
  ;({ wrapper } = await mountNotebookWorkspace([makeMe.aNoteRealm.please()]))
  await vi.waitFor(() =>
    expect(pageBarCommand(noteMoreOptionsTitles.overflowMenu)).not.toBeNull()
  )
  await chooseFromMore(noteMoreOptionsTitles.assimilation)
  for (const width of [768, 1280, 390, 320]) {
    await page.viewport(width, 700)
    await toggleWorkspaceContents()
    await vi.waitFor(() => {
      expect(
        pageBarCommand(noteMoreOptionsTitles.assimilation)?.getAttribute(
          "aria-pressed"
        )
      ).toBe("true")
      expect(commandsSitInPageBar(width)).toBe(true)
      expect(Math.abs(commandRightGap())).toBeLessThanOrEqual(1)
      expect(
        document
          .querySelector('[data-testid="note-assimilation-modes"]')
          ?.checkVisibility()
      ).toBe(true)
    })
  }
})

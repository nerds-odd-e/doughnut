import makeMe from "donut-test-fixtures/makeMe"
import {
  noteMoreOptionsTitles,
  noteToolbarOverflowTitles,
} from "@/components/notes/widgets/noteMoreOptionsTitles"
import { NOTE_TOOLBAR_MORE_OPTIONS_ORDER } from "@/composables/noteToolbarOverflow"
import { notebookSidebarClosedPlugin } from "@tests/helpers/notebookSidebarTestProvide"
import { onlyPendingPopup } from "@tests/helpers/popupStackTestSupport"
import {
  mountNoteToolbar,
  openNoteToolbarOverflowMenu,
  overflowMenuItem,
  resetNoteToolbarTestState,
} from "@tests/notes/noteToolbarTestHelpers"
import type { VueWrapper } from "@vue/test-utils"
import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  onTestFinished,
  vi,
} from "vitest"
import { page, server } from "vitest/browser"

const titles = noteMoreOptionsTitles

const controlTitles = [
  ...NOTE_TOOLBAR_MORE_OPTIONS_ORDER.flatMap(noteToolbarOverflowTitles),
  titles.overflowMenu,
]

function visibleControls() {
  return [
    ...document.querySelectorAll<HTMLElement>("[data-note-toolbar] [title]"),
  ].filter(
    (el) => controlTitles.includes(el.title) && el.getClientRects().length > 0
  )
}

const visibleTitles = () => visibleControls().map((el) => el.title)

const rowTops = () => [
  ...new Set(visibleControls().map((el) => el.getBoundingClientRect().top)),
]

async function mountToolbarInNotebookColumnAt(width: number) {
  const original = server.config.browser.viewport
  onTestFinished(() => page.viewport(original.width, original.height))
  await page.viewport(width, 600)
  const column = document.createElement("main")
  column.className = "px-4 container mx-auto"
  document.body.appendChild(column)
  return mountNoteToolbar(makeMe.aNoteRealm.title("Dummy Title").please(), {
    plugin: notebookSidebarClosedPlugin(),
    attachTo: column,
  })
}

describe("NoteToolbar in the notebook page column", () => {
  let wrapper: VueWrapper

  beforeEach(() => {
    resetNoteToolbarTestState()
  })

  afterEach(() => {
    wrapper.unmount()
    document.body.innerHTML = ""
  })

  it("keeps every control on one row as a phone-wide screen widens and narrows", async () => {
    wrapper = await mountToolbarInNotebookColumnAt(390)
    await vi.waitFor(() => {
      expect(visibleTitles().at(-1)).toBe(titles.overflowMenu)
      expect(rowTops()).toHaveLength(1)
    })

    await page.viewport(414, 600)
    await vi.waitFor(() => expect(visibleTitles()).toContain(titles.mcqs))
    expect(rowTops()).toHaveLength(1)

    await page.viewport(390, 600)
    await vi.waitFor(() => expect(visibleTitles()).not.toContain(titles.mcqs))
    expect(rowTops()).toHaveLength(1)
  })

  it("shows all ten actions and no more options at 640px", async () => {
    wrapper = await mountToolbarInNotebookColumnAt(640)

    await vi.waitFor(() => expect(visibleTitles()).toHaveLength(10))
    expect(visibleTitles()).not.toContain(titles.overflowMenu)
  })

  it.each([320, 390])(
    "opens more options inside a %ipx-wide screen with a selectable action",
    async (width) => {
      wrapper = await mountToolbarInNotebookColumnAt(width)
      await vi.waitFor(() =>
        expect(visibleTitles()).toContain(titles.overflowMenu)
      )

      await openNoteToolbarOverflowMenu(wrapper)

      const menu = () =>
        overflowMenuItem(titles.delete)!
          .closest("[data-dropdown-portal-panel]")!
          .getBoundingClientRect()
      await vi.waitFor(() => {
        expect(menu().left).toBeGreaterThanOrEqual(0)
        expect(menu().right).toBeLessThanOrEqual(width)
      })

      await page.elementLocator(overflowMenuItem(titles.delete)!).click()
      expect(onlyPendingPopup()?.message).toBe(
        'Confirm to trash "Dummy Title"?'
      )
    }
  )
})

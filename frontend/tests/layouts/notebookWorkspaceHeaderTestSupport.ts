import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import type { NoteRealm } from "@generated/donut-backend-api"
import { noteShowLocation } from "@/routes/noteShowLocation"
import {
  noteMoreOptionsTitles,
  noteToolbarOverflowTitles,
} from "@/components/notes/widgets/noteMoreOptionsTitles"
import { NOTE_TOOLBAR_MORE_OPTIONS_ORDER } from "@/composables/noteToolbarOverflow"
import makeMe from "donut-test-fixtures/makeMe"
import helper, {
  mockNotebookGetForNoteRealm,
  mockSdkService,
  productionRouterAt,
} from "@tests/helpers"
import {
  mockShowNoteForRealms,
  stubNotebookFolderListings,
} from "@tests/notes/sidebar/sidebarFolderListingSupport"
import { invalidateSidebarListingCache } from "@/components/notes/sidebarFolderListingCache"
import { overflowMenuItem } from "@tests/notes/noteToolbarTestHelpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { onTestFinished } from "vitest"
import { page, server } from "vitest/browser"
import { defineComponent, h } from "vue"
import { RouterView, type RouteLocationRaw } from "vue-router"

const App = defineComponent({ render: () => h(RouterView) })

export async function atViewportWidth(width: number) {
  const original = server.config.browser.viewport
  onTestFinished(() => page.viewport(original.width, original.height))
  await page.viewport(width, 700)
}

/**
 * The real notebook layout routed at `location`, with every note in
 * `noteRealms` served by the mocked backend.
 */
export async function mountNotebookWorkspace(
  noteRealms: NoteRealm[],
  location: RouteLocationRaw = noteShowLocation(noteRealms[0]!.id)
): Promise<{
  wrapper: VueWrapper
  router: Awaited<ReturnType<typeof productionRouterAt>>
}> {
  invalidateSidebarListingCache()
  mockShowNoteForRealms(noteRealms)
  mockNotebookGetForNoteRealm(noteRealms[0]!)
  stubNotebookFolderListings({})
  mockSdkService(NoteController, "getNoteInfo", makeMe.aNoteRecallInfo.please())
  const router = await productionRouterAt(location)
  const wrapper = helper
    .component(App)
    .withRealRouter(router)
    .withRoutedPages()
    .withCleanStorage()
    .withCurrentUser(makeMe.aUser.please())
    .mount({ attachTo: document.body })
  await flushPromises()
  return { wrapper, router }
}

/** The shared page bar: the row that holds the Donut menu. */
export function pageBarEl() {
  return document.querySelector(".donut-menu")!.closest("nav")!
}

export const noteCommandTitlesInOrder = [
  ...NOTE_TOOLBAR_MORE_OPTIONS_ORDER.flatMap(noteToolbarOverflowTitles),
  noteMoreOptionsTitles.overflowMenu,
]

export function noteCommandEls() {
  return [
    ...document.querySelectorAll<HTMLElement>("[data-note-toolbar] [title]"),
  ].filter(
    (el) =>
      noteCommandTitlesInOrder.includes(el.title) &&
      el.getClientRects().length > 0
  )
}

export const visibleCommandTitles = () => noteCommandEls().map((el) => el.title)

/** Commands use the right edge of the area allocated to the note toolbar. */
export function commandRightGap() {
  const toolbar = document.querySelector<HTMLElement>("[data-note-toolbar]")!
  const commands = noteCommandEls()
  return (
    toolbar.getBoundingClientRect().right -
    commands[commands.length - 1]!.getBoundingClientRect().right
  )
}

/** Whether every visible note command sits inside the page bar's row. */
export function commandsSitInPageBar(viewportWidth: number) {
  const bar = pageBarEl().getBoundingClientRect()
  return noteCommandEls().every((el) => {
    const r = el.getBoundingClientRect()
    return (
      r.top >= bar.top &&
      r.bottom <= bar.bottom &&
      r.left >= 0 &&
      r.right <= viewportWidth
    )
  })
}

export function pageBarCommand(title: string) {
  return document.querySelector<HTMLElement>(
    `[data-note-toolbar] [title="${title}"]`
  )
}

export async function clickPageBarCommand(title: string) {
  await page.elementLocator(pageBarCommand(title)!).click()
  await flushPromises()
}

export async function chooseFromMore(title: string) {
  await clickPageBarCommand(noteMoreOptionsTitles.overflowMenu)
  await page.elementLocator(overflowMenuItem(title)!).click()
  await flushPromises()
}

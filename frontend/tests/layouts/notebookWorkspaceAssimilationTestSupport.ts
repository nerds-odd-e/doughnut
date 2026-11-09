import makeMe from "donut-test-fixtures/makeMe"
import { notePropertyLocation } from "@/routes/noteShowLocation"
import { flushPromises } from "@vue/test-utils"
import { expect, vi } from "vitest"
import { page } from "vitest/browser"
import type { Router } from "vue-router"
import { pageBarEl } from "./notebookWorkspaceHeaderTestSupport"

export const activity = () =>
  pageBarEl().querySelector<HTMLButtonElement>(".nav-item button")!
export const expectActivity = async (label: string) =>
  vi.waitFor(() => {
    expect(activity().getAttribute("aria-label")).toBe(label)
    expect(activity().checkVisibility()).toBe(true)
  })
export const click = async (selector: string) => {
  await page.elementLocator(document.querySelector(selector)!).click()
  await flushPromises()
}
export const isVisible = (selector: string) =>
  document.querySelector<HTMLElement>(selector)?.checkVisibility() === true
export const panelSelector = '[data-testid="note-assimilation-modes"]'
export const inPanel = (dataTest: string) =>
  `${panelSelector} [data-test="${dataTest}"]`
export const clickInPanel = (dataTest: string) => click(inPanel(dataTest))
export const propertyNote = (readonly = false) =>
  makeMe.aNoteRealm
    .content("---\ntopic: training\nnote_level: 1\n---\n\nBody.")
    .readonly(readonly)
    .please()
/** The route reaches the note property and its own learning panel shows. */
export const expectPropertyLearningOpen = async (
  router: Router,
  noteId: number,
  propertyKey: string
) => {
  await vi.waitFor(() =>
    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(noteId, propertyKey)
    )
  )
  await vi.waitFor(() =>
    expect(
      isVisible(
        `[data-property-key="${propertyKey}"] [data-testid="rich-note-property-panel"]`
      )
    ).toBe(true)
  )
}

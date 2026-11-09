import type { MemoryTracker } from "@generated/donut-backend-api"
import {
  AssimilationController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { notePropertyLocation } from "@/routes/noteShowLocation"
import { mockSdkServiceWithImplementation } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { expect, vi } from "vitest"
import { page } from "vitest/browser"
import type { Router } from "vue-router"
import {
  clickPageBarCommand,
  pageBarEl,
} from "./notebookWorkspaceHeaderTestSupport"

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
export const rect = (selector: string) =>
  document.querySelector(selector)!.getBoundingClientRect()
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

export const nextRecallAt = "2026-11-20T08:00:00.000Z"
const nextRecallText = `Next ${new Date(nextRecallAt).toLocaleDateString(
  undefined,
  { day: "numeric", month: "short" }
)}`
/** Backend trackers per note: assimilating adds to them, note info reads them. */
export const serveNoteTrackers = (
  trackers: Record<number, MemoryTracker[]> = {}
) => {
  mockSdkServiceWithImplementation(NoteController, "getNoteInfo", ({ path }) =>
    makeMe.aNoteRecallInfo.memoryTrackers(trackers[path.note] ?? []).please()
  )
  mockSdkServiceWithImplementation(
    AssimilationController,
    "assimilate",
    ({ body: { noteId, assimilateAsCommissioned, assimilateAsSpelling } }) => {
      const tracker = makeMe.aMemoryTracker.nextRecallAt(nextRecallAt)
      if (assimilateAsCommissioned) tracker.commissioned()
      if (assimilateAsSpelling) tracker.spelling()
      const created = [tracker.please()]
      trackers[noteId!] = [...(trackers[noteId!] ?? []), ...created]
      return created
    }
  )
}
export const visibleText = (selector: string) => {
  const el = document.querySelector<HTMLElement>(selector)
  return el?.checkVisibility() ? el.textContent?.trim() : undefined
}
export const expectUnderstandingResult = () =>
  vi.waitFor(() =>
    expect(visibleText(`${inPanel("understanding-result")} p`)).toBe(
      "Understanding added to recall"
    )
  )
export const expectTrackerStatus = (mode: string) =>
  vi.waitFor(() =>
    expect(
      visibleText(`${inPanel(`assimilation-status-${mode}`)} + span`)
    ).toBe(nextRecallText)
  )
/** Opens the note's assimilation and waits for its default action. */
export const openNoteAssimilation = async (
  choose: (title: string) => Promise<void> = clickPageBarCommand
) => {
  await choose(noteMoreOptionsTitles.assimilation)
  await vi.waitFor(() =>
    expect(visibleText(inPanel("assimilate-UNDERSTANDING"))).toBe(
      "Assimilate understanding"
    )
  )
}
/** Opens the note's assimilation and leaves it at the Understanding result. */
export const assimilateUnderstanding = async () => {
  await openNoteAssimilation()
  await clickInPanel("assimilate-UNDERSTANDING")
  await expectUnderstandingResult()
}

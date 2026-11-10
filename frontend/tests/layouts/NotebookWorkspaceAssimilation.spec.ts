import { AssimilationController } from "@generated/donut-backend-api/sdk.gen"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useRecallData } from "@/composables/useRecallData"
import {
  notePropertyItemLocation,
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { resetRecallData } from "@tests/helpers/recallDataTestSupport"
import { memoryTrackerLitesStub } from "@tests/toolbars/menuDataMocks"
import makeMe from "donut-test-fixtures/makeMe"
import type { VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, expect, it, vi } from "vitest"
import { page } from "vitest/browser"
import {
  atViewportWidth,
  clickPageBarCommand,
  mountNotebookWorkspace,
} from "./notebookWorkspaceHeaderTestSupport"
import {
  activity,
  click,
  expectActivity,
  expectPropertyLearningOpen,
  openNoteAssimilation,
  propertyNote,
  rect,
} from "./notebookWorkspaceAssimilationTestSupport"

let wrapper: VueWrapper

beforeEach(async () => {
  useAssimilationView().dismiss()
  resetRecallData()
  await atViewportWidth(1280)
})
afterEach(() => {
  wrapper?.unmount()
  document.body.innerHTML = ""
})

it("identifies an opened note panel, closes it, and restores Note when switching notes", async () => {
  const first = propertyNote()
  const second = makeMe.aNoteRealm.title("Next note").please()
  const mounted = await mountNotebookWorkspace([first, second])
  wrapper = mounted.wrapper
  await clickPageBarCommand(noteMoreOptionsTitles.assimilation)
  await expectActivity("Assimilate")
  await clickPageBarCommand(noteMoreOptionsTitles.assimilation)
  await expectActivity("Note")
  await clickPageBarCommand(noteMoreOptionsTitles.assimilation)
  await mounted.router.push(noteShowLocation(second.id))
  await expectActivity("Note")
})

it("reads the note title first, then the default Understanding action beside the optional modes", async () => {
  const realm = makeMe.aNoteRealm.title("かた と ほう").please()
  ;({ wrapper } = await mountNotebookWorkspace([realm]))
  await openNoteAssimilation()
  const background = (selector: string) =>
    getComputedStyle(document.querySelector(selector)!).backgroundColor
  const understanding = rect('[data-test="assimilate-UNDERSTANDING"]')
  const optional = ["SPELLING", "COMMISSIONED"].map((mode) => ({
    action: rect(`[data-test="assimilate-${mode}"]`),
    icon: rect(`[data-test="assimilation-mode-row-${mode}"] svg`),
    label: rect(`[data-test="assimilation-mode-row-${mode}"] strong`),
  }))

  expect(rect('[data-test="note-title"]').bottom).toBeLessThanOrEqual(
    Math.min(understanding.top, ...optional.map((o) => o.icon.top))
  )
  expect(rect('[aria-label="Note content"]').top).toBeGreaterThanOrEqual(
    Math.max(understanding.bottom, ...optional.map((o) => o.action.bottom))
  )
  expect(optional[0]!.icon.top).toBeLessThan(understanding.bottom)
  for (const { icon, label } of optional) {
    expect(icon.left).toBeGreaterThanOrEqual(understanding.right)
    expect(icon.width).toBeGreaterThan(0)
    expect(icon.right).toBeLessThanOrEqual(label.left)
  }
  expect(background('[data-test="assimilate-UNDERSTANDING"]')).not.toBe(
    background('[data-test="assimilate-SPELLING"]')
  )
  expect(background('[data-test="assimilate-SPELLING"]')).toBe(
    background('[data-test="assimilate-COMMISSIONED"]')
  )
})

it("follows direct property learning, close, note identity and route lifetime", async () => {
  const realm = propertyNote()
  const second = makeMe.aNoteRealm.title("Other").please()
  const mounted = await mountNotebookWorkspace(
    [realm, second],
    notePropertyLocation(realm.id, "topic")
  )
  wrapper = mounted.wrapper
  await expectActivity("Assimilate")
  await click(
    '[data-property-key="topic"] [data-testid="rich-note-property-panel-toggle"]'
  )
  await expectActivity("Note")
  await click(
    '[data-property-key="topic"] [data-testid="rich-note-property-panel-toggle"]'
  )
  await expectActivity("Assimilate")
  await mounted.router.push(noteShowLocation(second.id))
  await expectActivity("Note")
  await mounted.router.push({ name: "bazaar" })
  await expectActivity("Bazaar")
})

it.each([
  { key: "missing", readonly: false },
  { key: "note_level", readonly: false },
  { key: "topic", readonly: true },
])(
  "keeps ordinary Note context for $key browsing (readonly: $readonly)",
  async ({ key, readonly }) => {
    const realm = propertyNote(readonly)
    ;({ wrapper } = await mountNotebookWorkspace(
      [realm],
      notePropertyLocation(realm.id, key)
    ))
    await expectActivity("Note")
  }
)

it("keeps eligible Resume ahead of visible property learning", async () => {
  const realm = propertyNote()
  ;({ wrapper } = await mountNotebookWorkspace(
    [realm],
    notePropertyLocation(realm.id, "topic")
  ))
  await expectActivity("Assimilate")
  const recall = useRecallData()
  recall.setToRepeat(memoryTrackerLitesStub(3))
  recall.setCurrentIndex(1)
  await expectActivity("Resume")
})

it("follows next-unit note to property destinations through the compact direct action and updates its badge", async () => {
  const first = makeMe.aNoteRealm.title("First").please()
  const second = propertyNote()
  const counts = {
    dueCount: 7,
    assimilatedCountOfTheDay: 2,
    totalUnassimilatedCount: 12,
  }
  const next = mockSdkService(AssimilationController, "next", {
    nextUnit: { noteId: first.id },
    counts,
  })
  const mounted = await mountNotebookWorkspace([first, second])
  wrapper = mounted.wrapper
  await click('[aria-label="Donut menu"]')
  await click('[aria-label="Donut menu panel"] [aria-label="Assimilate"]')
  await expectActivity("Assimilate")
  expect(activity().querySelector(".due-count")?.textContent?.trim()).toBe("7")
  next.mockResolvedValue(
    wrapSdkResponse({
      nextUnit: { noteId: second.id, propertyKey: "topic", propertyValue: "" },
      counts: { ...counts, dueCount: 6 },
    })
  )
  await page.elementLocator(activity()).click()
  await expectActivity("Assimilate")
  await expectPropertyLearningOpen(mounted.router, second.id, "topic")
  expect(activity().querySelector(".due-count")?.textContent?.trim()).toBe("6")
})

it("continues to the first matching occurrence of the returned property value", async () => {
  const first = makeMe.aNoteRealm.title("First").please()
  const second = makeMe.aNoteRealm
    .content("---\nactivity: [walk, run, run]\n---\nBody.")
    .please()
  const next = mockSdkService(AssimilationController, "next", {
    nextUnit: { noteId: first.id },
    counts: { dueCount: 2 },
  })
  const mounted = await mountNotebookWorkspace([first, second])
  wrapper = mounted.wrapper
  await click('[aria-label="Donut menu"]')
  await click('[aria-label="Donut menu panel"] [aria-label="Assimilate"]')
  next.mockResolvedValue(
    wrapSdkResponse({
      nextUnit: {
        noteId: second.id,
        propertyKey: "activity",
        propertyValue: "run",
      },
      counts: { dueCount: 1 },
    })
  )
  await page.elementLocator(activity()).click()
  await vi.waitFor(() => {
    expect(mounted.router.currentRoute.value).toMatchObject(
      notePropertyItemLocation(second.id, "activity", {
        value: "run",
        occurrence: 1,
      })
    )
    const rows = document.querySelectorAll(
      '[data-property-key="activity"] [data-test="assimilation-mode-row-UNDERSTANDING"]'
    )
    expect(rows[1]?.getAttribute("data-property-value-focused")).toBe("true")
  })
})

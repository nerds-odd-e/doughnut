import { AssimilationController } from "@generated/donut-backend-api/sdk.gen"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useRecallData } from "@/composables/useRecallData"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { resetRecallData } from "@tests/helpers/recallDataTestSupport"
import { memoryTrackerLitesStub } from "@tests/toolbars/menuDataMocks"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, expect, it, vi } from "vitest"
import { page } from "vitest/browser"
import {
  atViewportWidth,
  clickPageBarCommand,
  mountNotebookWorkspace,
  pageBarEl,
} from "./notebookWorkspaceHeaderTestSupport"

let wrapper: VueWrapper
const activity = () =>
  pageBarEl().querySelector<HTMLButtonElement>(".nav-item button")!
const expectActivity = async (label: string) =>
  vi.waitFor(() => {
    expect(activity().getAttribute("aria-label")).toBe(label)
    expect(activity().checkVisibility()).toBe(true)
  })
const click = async (selector: string) => {
  await page.elementLocator(document.querySelector(selector)!).click()
  await flushPromises()
}
const propertyNote = (readonly = false) =>
  makeMe.aNoteRealm
    .content("---\ntopic: training\nnote_level: 1\n---\n\nBody.")
    .readonly(readonly)
    .please()

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
      nextUnit: { noteId: second.id, propertyKey: "topic" },
      counts: { ...counts, dueCount: 6 },
    })
  )
  await page.elementLocator(activity()).click()
  await expectActivity("Assimilate")
  await vi.waitFor(() =>
    expect(mounted.router.currentRoute.value).toMatchObject(
      notePropertyLocation(second.id, "topic")
    )
  )
  expect(
    document
      .querySelector<HTMLElement>(
        '[data-property-key="topic"] [data-testid="rich-note-property-panel"]'
      )
      ?.checkVisibility()
  ).toBe(true)
  expect(activity().querySelector(".due-count")?.textContent?.trim()).toBe("6")
})

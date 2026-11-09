import type { MemoryTracker } from "@generated/donut-backend-api"
import {
  AssimilationController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import { useAssimilationView } from "@/composables/useAssimilationView"
import {
  DAILY_GOAL_TOAST,
  NO_MORE_TOAST,
} from "@/composables/useGoToNextAssimilation"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import { resetRecallData } from "@tests/helpers/recallDataTestSupport"
import {
  showToastsOnPage,
  toastMessagesOnPage,
} from "@tests/helpers/toastTestSupport"
import makeMe from "donut-test-fixtures/makeMe"
import type { VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, expect, it, vi } from "vitest"
import {
  atViewportWidth,
  clickPageBarCommand,
  mountNotebookWorkspace,
} from "./notebookWorkspaceHeaderTestSupport"
import {
  click,
  expectActivity,
  propertyNote,
} from "./notebookWorkspaceAssimilationTestSupport"

let wrapper: VueWrapper

const nextRecallAt = "2026-11-20T08:00:00.000Z"
const nextRecallText = `Next ${new Date(nextRecallAt).toLocaleDateString(
  undefined,
  { day: "numeric", month: "short" }
)}`
/** Backend trackers per note: assimilating adds to them, note info reads them. */
const serveNoteTrackers = (trackers: Record<number, MemoryTracker[]> = {}) => {
  mockSdkServiceWithImplementation(NoteController, "getNoteInfo", ({ path }) =>
    makeMe.aNoteRecallInfo.memoryTrackers(trackers[path.note] ?? []).please()
  )
  mockSdkServiceWithImplementation(
    AssimilationController,
    "assimilate",
    ({ body: { noteId, assimilateAsCommissioned } }) => {
      const tracker = makeMe.aMemoryTracker.nextRecallAt(nextRecallAt)
      const created = [
        assimilateAsCommissioned
          ? tracker.commissioned().please()
          : tracker.please(),
      ]
      trackers[noteId!] = [...(trackers[noteId!] ?? []), ...created]
      return created
    }
  )
}
const panelSelector = '[data-testid="note-assimilation-modes"]'
const clickInPanel = (dataTest: string) =>
  click(`${panelSelector} [data-test="${dataTest}"]`)
const visibleText = (selector: string) => {
  const el = document.querySelector<HTMLElement>(selector)
  return el?.checkVisibility() ? el.textContent?.trim() : undefined
}
const expectUnderstandingResult = () =>
  vi.waitFor(() =>
    expect(
      visibleText(`${panelSelector} [data-test="understanding-result"] p`)
    ).toBe("Understanding added to recall")
  )
const expectTrackerStatus = (mode: string) =>
  vi.waitFor(() =>
    expect(
      visibleText(
        `${panelSelector} [data-test="assimilation-status-${mode}"] + span`
      )
    ).toBe(nextRecallText)
  )
/** Opens the note's assimilation and leaves it at the Understanding result. */
const assimilateUnderstanding = async () => {
  await clickPageBarCommand(noteMoreOptionsTitles.assimilation)
  await vi.waitFor(() =>
    expect(
      visibleText(`${panelSelector} [data-test="assimilate-UNDERSTANDING"]`)
    ).toBe("Assimilate understanding")
  )
  await clickInPanel("assimilate-UNDERSTANDING")
  await expectUnderstandingResult()
}

showToastsOnPage()

beforeEach(async () => {
  useAssimilationView().dismiss()
  resetRecallData()
  await atViewportWidth(1280)
})
afterEach(() => {
  wrapper?.unmount()
  document.body.innerHTML = ""
})

it("keeps this visit's Understanding result on the note through an optional addition", async () => {
  const realm = makeMe.aNoteRealm.title("かた と ほう").please()
  const earlier = makeMe.aNoteRealm.title("Earlier").please()
  const mounted = await mountNotebookWorkspace([realm, earlier])
  wrapper = mounted.wrapper
  serveNoteTrackers({
    [earlier.id]: [makeMe.aMemoryTracker.nextRecallAt(nextRecallAt).please()],
  })

  await assimilateUnderstanding()

  expect(visibleText('[data-test="note-title"]')).toBe("かた と ほう")
  expect(mounted.router.currentRoute.value).toMatchObject(
    noteShowLocation(realm.id)
  )
  const panel = document.querySelector(panelSelector)!

  await clickInPanel("assimilate-COMMISSIONED")
  await expectTrackerStatus("COMMISSIONED")

  expect(panel.isConnected).toBe(false)
  await expectUnderstandingResult()
  expect(mounted.router.currentRoute.value).toMatchObject(
    noteShowLocation(realm.id)
  )

  await mounted.router.push(noteShowLocation(earlier.id))
  await vi.waitFor(() =>
    expect(visibleText('[data-test="note-title"]')).toBe("Earlier")
  )
  await clickPageBarCommand(noteMoreOptionsTitles.assimilation)
  await expectTrackerStatus("UNDERSTANDING")
})

it("Next item opens the next note, also once the daily goal is reached", async () => {
  const realm = makeMe.aNoteRealm.title("First").please()
  const next = makeMe.aNoteRealm.title("Next note").please()
  const mounted = await mountNotebookWorkspace([realm, next])
  wrapper = mounted.wrapper
  serveNoteTrackers()
  mockSdkService(AssimilationController, "next", {
    nextUnit: { noteId: next.id },
    counts: { dueCount: 0 },
  })
  await assimilateUnderstanding()

  await clickInPanel("next-item")

  await vi.waitFor(() =>
    expect(
      visibleText(`${panelSelector} [data-test="assimilate-UNDERSTANDING"]`)
    ).toBe("Assimilate understanding")
  )
  expect(mounted.router.currentRoute.value).toMatchObject(
    noteShowLocation(next.id)
  )
  expect(visibleText('[data-test="note-title"]')).toBe("Next note")
  expect(toastMessagesOnPage()).toEqual([DAILY_GOAL_TOAST])
})

it("Next item opens the next unit when it is a property of the same note", async () => {
  const realm = propertyNote()
  const mounted = await mountNotebookWorkspace([realm])
  wrapper = mounted.wrapper
  serveNoteTrackers()
  mockSdkService(AssimilationController, "next", {
    nextUnit: { noteId: realm.id, propertyKey: "topic" },
  })
  await assimilateUnderstanding()

  await clickInPanel("next-item")

  await vi.waitFor(() =>
    expect(mounted.router.currentRoute.value).toMatchObject(
      notePropertyLocation(realm.id, "topic")
    )
  )
  await vi.waitFor(() =>
    expect(
      document
        .querySelector<HTMLElement>(
          '[data-property-key="topic"] [data-testid="rich-note-property-panel"]'
        )
        ?.checkVisibility()
    ).toBe(true)
  )
})

it("Next item with nothing left keeps the note, says so and closes assimilation", async () => {
  const realm = makeMe.aNoteRealm.title("Last").please()
  const mounted = await mountNotebookWorkspace([realm])
  wrapper = mounted.wrapper
  serveNoteTrackers()
  mockSdkService(AssimilationController, "next", {})
  await assimilateUnderstanding()

  await clickInPanel("next-item")

  await vi.waitFor(() => expect(toastMessagesOnPage()).toEqual([NO_MORE_TOAST]))
  await expectActivity("Note")
  expect(mounted.router.currentRoute.value).toMatchObject(
    noteShowLocation(realm.id)
  )
  expect(visibleText('[data-test="note-title"]')).toBe("Last")
})

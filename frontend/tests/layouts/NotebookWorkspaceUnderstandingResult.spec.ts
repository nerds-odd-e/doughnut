import { AssimilationController } from "@generated/donut-backend-api/sdk.gen"
import { useAssimilationView } from "@/composables/useAssimilationView"
import {
  DAILY_GOAL_TOAST,
  NO_MORE_TOAST,
} from "@/composables/useGoToNextAssimilation"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { noteMoreOptionsTitles } from "@/components/notes/widgets/noteMoreOptionsTitles"
import { mockSdkService } from "@tests/helpers"
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
  assimilateUnderstanding,
  clickInPanel,
  expectActivity,
  expectPropertyLearningOpen,
  expectTrackerStatus,
  expectUnderstandingResult,
  inPanel,
  nextRecallAt,
  openNoteAssimilation,
  panelSelector,
  propertyNote,
  serveNoteTrackers,
  showNoteCallCount,
  visibleText,
} from "./notebookWorkspaceAssimilationTestSupport"

let wrapper: VueWrapper

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

  await openNoteAssimilation()
  const panel = document.querySelector(panelSelector)!
  const showNoteCount = showNoteCallCount()

  await clickInPanel("assimilate-UNDERSTANDING")
  await expectUnderstandingResult()
  expect(panel.isConnected).toBe(true)
  expect(showNoteCallCount()).toBe(showNoteCount)

  expect(visibleText('[data-test="note-title"]')).toBe("かた と ほう")
  expect(mounted.router.currentRoute.value).toMatchObject(
    noteShowLocation(realm.id)
  )

  await clickInPanel("assimilate-COMMISSIONED")
  await expectTrackerStatus("COMMISSIONED")

  expect(panel.isConnected).toBe(true)
  expect(showNoteCallCount()).toBe(showNoteCount)
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
    expect(visibleText(inPanel("assimilate-UNDERSTANDING"))).toBe(
      "Assimilate understanding"
    )
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

  await expectPropertyLearningOpen(mounted.router, realm.id, "topic")
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

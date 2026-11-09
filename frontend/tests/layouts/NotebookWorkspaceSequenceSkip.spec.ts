import type { NoteRealm } from "@generated/donut-backend-api"
import {
  AssimilationController,
  AssimilationSequenceSkipController,
} from "@generated/donut-backend-api/sdk.gen"
import Popups from "@/components/commons/Popups/Popups.vue"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { NO_MORE_TOAST } from "@/composables/useGoToNextAssimilation"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { mockSdkService } from "@tests/helpers"
import { resetRecallData } from "@tests/helpers/recallDataTestSupport"
import {
  showToastsOnPage,
  toastMessagesOnPage,
} from "@tests/helpers/toastTestSupport"
import makeMe from "donut-test-fixtures/makeMe"
import { mount, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, expect, it, vi } from "vitest"
import { page } from "vitest/browser"
import {
  atViewportWidth,
  mountNotebookWorkspace,
} from "./notebookWorkspaceHeaderTestSupport"
import {
  clickInPanel,
  expectActivity,
  expectPropertyLearningOpen,
  inPanel,
  isVisible,
  openNoteAssimilation,
  propertyNote,
  showNoteCallCount,
  visibleText,
} from "./notebookWorkspaceAssimilationTestSupport"

showToastsOnPage()

let wrappers: VueWrapper[] = []
let skipSpy: ReturnType<typeof mockSdkService>

const confirmation = () =>
  document.querySelector<HTMLDialogElement>("dialog[open]")
const answerConfirmation = async (label: "OK" | "Cancel") => {
  const button = [...confirmation()!.querySelectorAll("button")].find(
    (b) => b.textContent?.trim() === label
  )!
  await page.elementLocator(button).click()
}

/** Opens the note's assimilation in the workspace, with the app's popup host, and clicks Skip from sequence…. */
const openSkipConfirmation = async (realm: NoteRealm) => {
  const mounted = await mountNotebookWorkspace([realm])
  wrappers.push(
    mounted.wrapper,
    mount(Popups, {
      global: { plugins: [mounted.router] },
      attachTo: document.body,
    })
  )
  await openNoteAssimilation()
  await clickInPanel("skip")
  await vi.waitFor(() => expect(confirmation()?.checkVisibility()).toBe(true))
  return mounted.router
}

beforeEach(async () => {
  useAssimilationView().dismiss()
  resetRecallData()
  await atViewportWidth(1280)
  skipSpy = mockSdkService(AssimilationSequenceSkipController, "create", {
    id: 1,
  })
})
afterEach(() => {
  for (const wrapper of wrappers) wrapper.unmount()
  wrappers = []
  document.body.innerHTML = ""
})

it("Skip from sequence… explains that only the note's Understanding stays out, and Cancel keeps the note offered", async () => {
  const realm = propertyNote()
  const router = await openSkipConfirmation(realm)

  expect(confirmation()!.querySelector(".modal-body span")?.textContent).toBe(
    "Only this note's Understanding will stay out of the assimilation sequence, until you choose Return to sequence. Its properties and optional modes are not affected, so a property of this note may come next."
  )

  await answerConfirmation("Cancel")

  await vi.waitFor(() => expect(confirmation()).toBeNull())
  expect(skipSpy).not.toHaveBeenCalled()
  expect(router.currentRoute.value).toMatchObject(noteShowLocation(realm.id))
  expect(isVisible(inPanel("skip"))).toBe(true)
})

it("confirming from the bottom bar skips only the note's Understanding and opens its property when that comes next", async () => {
  await atViewportWidth(768)
  const realm = propertyNote()
  mockSdkService(AssimilationController, "next", {
    nextUnit: { noteId: realm.id, propertyKey: "topic" },
  })
  const router = await openSkipConfirmation(realm)

  await answerConfirmation("OK")

  await expectPropertyLearningOpen(router, realm.id, "topic")
  expect(skipSpy).toHaveBeenCalledWith({ body: { noteId: realm.id } })
})

it("confirming Skip with nothing next keeps the note without reloading it", async () => {
  const realm = makeMe.aNoteRealm.title("Last").please()
  mockSdkService(AssimilationController, "next", {})
  const router = await openSkipConfirmation(realm)
  const showNoteCount = showNoteCallCount()

  await answerConfirmation("OK")

  await vi.waitFor(() => expect(toastMessagesOnPage()).toEqual([NO_MORE_TOAST]))
  await expectActivity("Note")
  expect(showNoteCallCount()).toBe(showNoteCount)
  expect(router.currentRoute.value).toMatchObject(noteShowLocation(realm.id))
  expect(visibleText('[data-test="note-title"]')).toBe("Last")
  expect(skipSpy).toHaveBeenCalledWith({ body: { noteId: realm.id } })
})

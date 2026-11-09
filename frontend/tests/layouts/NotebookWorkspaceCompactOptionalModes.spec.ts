import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { mockSdkService } from "@tests/helpers"
import { resetRecallData } from "@tests/helpers/recallDataTestSupport"
import makeMe from "donut-test-fixtures/makeMe"
import type { VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, expect, it, vi } from "vitest"
import { page, userEvent } from "vitest/browser"
import {
  atViewportWidth,
  chooseFromMore,
  mountNotebookWorkspace,
  toggleWorkspaceContents,
} from "./notebookWorkspaceHeaderTestSupport"
import {
  assimilateUnderstanding,
  click,
  clickInPanel,
  expectTrackerStatus,
  expectUnderstandingResult,
  inPanel,
  isVisible,
  nextRecallAt,
  openNoteAssimilation,
  panelSelector,
  rect,
  serveNoteTrackers,
  visibleText,
} from "./notebookWorkspaceAssimilationTestSupport"

let wrapper: VueWrapper

const trigger = () =>
  document.querySelector<HTMLElement>(inPanel("optional-modes"))!
const optionalModesShown = () =>
  ["SPELLING", "COMMISSIONED"].every((mode) =>
    isVisible(inPanel(`assimilation-mode-row-${mode}`))
  )
const openOptionalModes = async () => {
  await clickInPanel("optional-modes")
  await vi.waitFor(() => expect(optionalModesShown()).toBe(true))
}
const expectClosedAtTrigger = () =>
  vi.waitFor(() => {
    expect(optionalModesShown()).toBe(false)
    expect(document.activeElement).toBe(trigger())
  })
/** The colour the learner sees at a point of the viewport. */
const colourOnScreenAt = async (x: number, y: number) => {
  const image = new Image()
  image.src = `data:image/png;base64,${await page.screenshot({ save: false })}`
  await image.decode()
  const canvas = document.createElement("canvas")
  canvas.width = image.width
  canvas.height = image.height
  const context = canvas.getContext("2d")!
  context.drawImage(image, 0, 0)
  const scale = image.width / window.innerWidth
  return [...context.getImageData(x * scale, y * scale, 1, 1).data.slice(0, 3)]
}

beforeEach(async () => {
  useAssimilationView().dismiss()
  resetRecallData()
  await atViewportWidth(768)
})
afterEach(() => {
  wrapper?.unmount()
  document.body.innerHTML = ""
})

it("on a phone, one tap on Optional modes shows each optional mode's action or tracker, and Escape or a click outside returns to the trigger", async () => {
  await atViewportWidth(375)
  const realm = makeMe.aNoteRealm.title("かた と ほう").please()
  const mounted = await mountNotebookWorkspace([
    makeMe.aNoteRealm.please(),
    realm,
  ])
  wrapper = mounted.wrapper
  serveNoteTrackers({
    [realm.id]: [
      makeMe.aMemoryTracker.nextRecallAt(nextRecallAt).commissioned().please(),
    ],
  })
  await mounted.router.push(noteShowLocation(realm.id))
  await vi.waitFor(() =>
    expect(visibleText('[data-test="note-title"]')).toBe("かた と ほう")
  )
  await openNoteAssimilation(chooseFromMore)
  expect(optionalModesShown()).toBe(false)

  await openOptionalModes()

  const spelling = inPanel("assimilation-mode-row-SPELLING")
  expect(visibleText(`${panelSelector} details h4`)).toBe("Optional modes")
  expect(isVisible(`${spelling} svg`)).toBe(true)
  expect(visibleText(`${spelling} strong`)).toBe("Spelling")
  expect(visibleText(`${spelling} p`)).toBe(
    "Recall the title or an alias from memory."
  )
  expect(visibleText(inPanel("assimilate-SPELLING"))).toBe("Verify spelling")
  await expectTrackerStatus("COMMISSIONED")
  const popover = rect(`${panelSelector} .daisy-dropdown-content`)
  expect(popover.left).toBeGreaterThanOrEqual(0)
  expect(popover.right).toBeLessThanOrEqual(window.innerWidth)
  expect(rect(inPanel("assimilate-SPELLING")).top).toBeGreaterThanOrEqual(
    rect(`${spelling} p`).bottom
  )

  await userEvent.tab()
  expect(document.activeElement).toBe(
    document.querySelector(inPanel("assimilate-SPELLING"))
  )
  await userEvent.keyboard("{Escape}")
  await expectClosedAtTrigger()

  await openOptionalModes()
  await click(`${panelSelector} h3`)
  await expectClosedAtTrigger()
})

it("adding Commissioned from Optional modes keeps the note and its Understanding result", async () => {
  const realm = makeMe.aNoteRealm.title("かた と ほう").please()
  const mounted = await mountNotebookWorkspace([realm])
  wrapper = mounted.wrapper
  serveNoteTrackers()
  await assimilateUnderstanding()
  await openOptionalModes()

  await clickInPanel("assimilate-COMMISSIONED")

  await expectUnderstandingResult()
  expect(mounted.router.currentRoute.value).toMatchObject(
    noteShowLocation(realm.id)
  )
  await openOptionalModes()
  await expectTrackerStatus("COMMISSIONED")
})

it("Verify spelling from Optional modes closes the popover and covers the note until it is cancelled or verified", async () => {
  const realm = makeMe.aNoteRealm.title("かた と ほう").please()
  const mounted = await mountNotebookWorkspace([realm])
  wrapper = mounted.wrapper
  serveNoteTrackers()
  mockSdkService(NoteController, "verifySpelling", { correct: true })
  await openNoteAssimilation()
  const blocker = () =>
    document.querySelector('[data-test="opaque-content-blocker"]')
  const verifySpelling = async () => {
    await openOptionalModes()
    await clickInPanel("assimilate-SPELLING")
    await vi.waitFor(() => expect(blocker()?.checkVisibility()).toBe(true))
  }

  await verifySpelling()

  expect(optionalModesShown()).toBe(false)
  const { left, top, right, bottom } = blocker()!.getBoundingClientRect()
  expect([left, top, right, bottom]).toEqual([
    0,
    0,
    window.innerWidth,
    window.innerHeight,
  ])
  const understanding = rect(inPanel("assimilate-UNDERSTANDING"))
  expect(
    await colourOnScreenAt(
      (understanding.left + understanding.right) / 2,
      (understanding.top + understanding.bottom) / 2
    )
  ).toEqual([0, 0, 0])
  await click(".close-button")
  await vi.waitFor(() => expect(blocker()).toBeNull())
  expect(visibleText('[data-test="note-title"]')).toBe("かた と ほう")

  await verifySpelling()
  await userEvent.fill(
    document.querySelector<HTMLElement>(
      '[data-test="spelling-verification-input"]'
    )!,
    "かた と ほう"
  )
  await click('[data-test="verify-spelling"]')

  await vi.waitFor(() => expect(blocker()).toBeNull())
  expect(mounted.router.currentRoute.value).toMatchObject(
    noteShowLocation(realm.id)
  )
  await openOptionalModes()
  await expectTrackerStatus("SPELLING")
})

it("a wide window narrowed by the notebook sidebar shows the optional modes beside Understanding once the sidebar hides", async () => {
  await atViewportWidth(1024)
  const realm = makeMe.aNoteRealm.title("かた と ほう").please()
  ;({ wrapper } = await mountNotebookWorkspace([realm]))
  await openNoteAssimilation()

  await toggleWorkspaceContents()

  await vi.waitFor(() => expect(optionalModesShown()).toBe(true))
})

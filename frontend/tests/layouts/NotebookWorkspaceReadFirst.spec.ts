import { AssimilationController } from "@generated/donut-backend-api/sdk.gen"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { mockSdkService } from "@tests/helpers"
import { resetRecallData } from "@tests/helpers/recallDataTestSupport"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, expect, it, vi } from "vitest"
import {
  atViewportWidth,
  chooseFromMore,
  clickPageBarCommand,
  mountNotebookWorkspace,
  toggleWorkspaceContents,
} from "./notebookWorkspaceHeaderTestSupport"
import {
  clickInPanel,
  expectUnderstandingResult,
  inPanel,
  isVisible,
  openNoteAssimilation,
  panelSelector,
  rect,
  serveNoteTrackers,
  visibleText,
} from "./notebookWorkspaceAssimilationTestSupport"

let wrapper: VueWrapper

const lastLine = "The last line of this long note."
const longNote = () =>
  makeMe.aNoteRealm
    .title(
      "かた と ほう: a long title that wraps over several lines when the screen is narrow"
    )
    .content(
      [
        "---\ntopic: training\nnote_level: 1\n---",
        ...Array.from(
          { length: 30 },
          (_, i) => `Paragraph ${i + 1} of the note body.`
        ),
        lastLine,
      ].join("\n\n")
    )
    .please()

const bar = () => rect(panelSelector)
const lastLineEl = () =>
  [
    ...document.querySelectorAll<HTMLElement>('[aria-label="Note content"] p'),
  ].find((p) => p.textContent?.trim() === lastLine)!
/** On the screen and the topmost thing where the learner taps it. */
const expectOperable = (selector: string) => {
  const el = document.querySelector<HTMLElement>(selector)!
  const { left, top, right, bottom } = el.getBoundingClientRect()
  expect(el.checkVisibility()).toBe(true)
  expect(left).toBeGreaterThanOrEqual(0)
  expect(top).toBeGreaterThanOrEqual(0)
  expect(right).toBeLessThanOrEqual(window.innerWidth)
  expect(bottom).toBeLessThanOrEqual(window.innerHeight)
  expect(
    el.contains(
      document.elementFromPoint((left + right) / 2, (top + bottom) / 2)
    )
  ).toBe(true)
}
/**
 * Scrolls through to the end of the note. Once the note's own scrolling ends,
 * the learner's scroll carries on into the notebook column, which holds the
 * notebook location above the note while the sidebar is hidden.
 */
const scrollToEndOfNote = async () => {
  for (const scroller of [".note-content-wrapper", "main"]) {
    const el = document.querySelector(scroller)!
    el.scrollTop = el.scrollHeight
  }
  await new Promise(requestAnimationFrame)
}
const headingSize = () =>
  getComputedStyle(document.querySelector(`${panelSelector} h3`)!).fontSize

beforeEach(() => {
  useAssimilationView().dismiss()
  resetRecallData()
})
afterEach(() => {
  wrapper?.unmount()
  document.body.innerHTML = ""
})

it.each([
  { case: "a phone", width: 375, hideSidebar: false, choose: chooseFromMore },
  {
    case: "an iPad in portrait with the sidebar hidden",
    width: 768,
    hideSidebar: true,
    choose: clickPageBarCommand,
  },
  {
    case: "a 1024px window narrowed by the notebook sidebar",
    width: 1024,
    hideSidebar: false,
    choose: clickPageBarCommand,
  },
])(
  "on $case, the title, properties and content come first, and the bottom bar keeps the actions and Optional modes at hand to the note's last line",
  async ({ width, hideSidebar, choose }) => {
    await atViewportWidth(width)
    ;({ wrapper } = await mountNotebookWorkspace([longNote()]))
    if (hideSidebar) await toggleWorkspaceContents()
    await openNoteAssimilation(choose)

    const { top } = bar()
    expect(rect('[data-test="note-title"]').bottom).toBeLessThanOrEqual(
      rect('[data-property-key="topic"]').top
    )
    expect(rect('[data-property-key="topic"]').bottom).toBeLessThanOrEqual(top)
    expect(rect('[aria-label="Note content"] p').bottom).toBeLessThanOrEqual(
      top
    )
    for (const control of [
      "assimilate-UNDERSTANDING",
      "skip",
      "optional-modes",
    ])
      expectOperable(inPanel(control))
    expect(visibleText(inPanel("skip"))).toBe("Skip from sequence…")
    expect(headingSize()).toBe("16px")

    await scrollToEndOfNote()

    expect(lastLineEl().getBoundingClientRect().bottom).toBeLessThanOrEqual(
      bar().top
    )
    expect(lastLineEl().getBoundingClientRect().top).toBeGreaterThanOrEqual(
      rect(".note-content-wrapper").top
    )
    expectOperable(inPanel("assimilate-UNDERSTANDING"))
    expectOperable(inPanel("skip"))

    await clickInPanel("optional-modes")

    const popover = rect(`${panelSelector} .daisy-dropdown-content`)
    expect(popover.top).toBeGreaterThanOrEqual(0)
    expect(popover.bottom).toBeLessThanOrEqual(
      rect(inPanel("optional-modes")).top
    )
    for (const mode of ["SPELLING", "COMMISSIONED"])
      expectOperable(inPanel(`assimilate-${mode}`))
  }
)

it("on a phone, the bottom bar shows what Understanding added with Optional modes still at hand, and only Next item moves on", async () => {
  await atViewportWidth(375)
  const realm = longNote()
  const next = makeMe.aNoteRealm.title("Next note").please()
  const mounted = await mountNotebookWorkspace([realm, next])
  wrapper = mounted.wrapper
  serveNoteTrackers()
  mockSdkService(AssimilationController, "next", {
    nextUnit: { noteId: next.id },
  })
  await openNoteAssimilation(chooseFromMore)

  await clickInPanel("assimilate-UNDERSTANDING")

  await expectUnderstandingResult()
  expect(isVisible(`${inPanel("understanding-result")} svg`)).toBe(true)
  expect(rect('[data-test="note-title"]').bottom).toBeLessThanOrEqual(bar().top)
  expectOperable(inPanel("optional-modes"))
  expectOperable(inPanel("next-item"))
  expect(mounted.router.currentRoute.value).toMatchObject(
    noteShowLocation(realm.id)
  )

  await clickInPanel("next-item")

  await vi.waitFor(() =>
    expect(visibleText('[data-test="note-title"]')).toBe("Next note")
  )
  expectOperable(inPanel("assimilate-UNDERSTANDING"))
})

it("moves the same Understanding result between Visible options after the title and the bottom bar as the window narrows and widens", async () => {
  await atViewportWidth(1280)
  ;({ wrapper } = await mountNotebookWorkspace([longNote()]))
  serveNoteTrackers()
  await openNoteAssimilation()
  await clickInPanel("assimilate-UNDERSTANDING")
  await expectUnderstandingResult()
  const visibleOptionsAfterTitle = () => {
    expect(rect('[data-test="note-title"]').bottom).toBeLessThanOrEqual(
      bar().top
    )
    expect(bar().bottom).toBeLessThanOrEqual(
      rect('[aria-label="Note content"]').top
    )
    expect(isVisible(inPanel("assimilation-mode-row-SPELLING"))).toBe(true)
    expect(headingSize()).toBe("16px")
  }
  visibleOptionsAfterTitle()

  await atViewportWidth(768)

  await vi.waitFor(() =>
    expect(bar().top).toBeGreaterThanOrEqual(
      rect(".note-content-wrapper").bottom
    )
  )
  expectOperable(inPanel("next-item"))
  expectOperable(inPanel("optional-modes"))

  await atViewportWidth(1280)

  await vi.waitFor(visibleOptionsAfterTitle)
  await expectUnderstandingResult()
})

it("gives a read-only note no assimilation bar", async () => {
  await atViewportWidth(375)
  const realm = makeMe.aNoteRealm.title("Shared").readonly(true).please()
  ;({ wrapper } = await mountNotebookWorkspace([realm]))

  useAssimilationView().openForNote(realm.id)
  await flushPromises()

  await vi.waitFor(() =>
    expect(visibleText('[data-test="note-title"]')).toBe("Shared")
  )
  expect(document.querySelector(panelSelector)).toBeNull()
})

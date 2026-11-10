import {
  MemoryTrackerController,
  NoteController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import {
  answerOnlyPendingPopup,
  emptyPopupStack,
  pendingPopups,
} from "@tests/helpers/popupStackTestSupport"
import { showToastsOnPage, toastShown } from "@tests/helpers/toastTestSupport"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, beforeAll, expect, it, vi } from "vitest"
import {
  notePropertyItemLocation,
  notePropertyLocation,
} from "@/routes/noteShowLocation"
import { mountNoteTextContent } from "./noteTextContentTestSupport"

import { setupGlobalClient } from "@/managedApi/clientSetup"
beforeAll(() => setupGlobalClient({ states: [] }))
showToastsOnPage()
let wrapper: VueWrapper
const selected = '[data-property-value-focused="true"]'
afterEach(() => {
  wrapper?.unmount()
  emptyPopupStack()
  vi.restoreAllMocks()
})

async function mountItems(
  items = ["walk", "run", "jog"],
  focus = { value: "run", occurrence: 1 }
) {
  const content = `---\nactivity: ${JSON.stringify(items)}\nsource: local\n---\nBody.`
  const note = makeMe.aNote.content(content).please()
  const info = mockSdkService(NoteController, "getNoteInfo", {
    memoryTrackers: [],
  })
  const deletion = mockSdkService(MemoryTrackerController, "delete", undefined)
  const save = mockSdkServiceWithImplementation(
    TextContentController,
    "updateNoteContent",
    async (options) => makeMe.aNoteRealm.content(options.body!.content).please()
  )
  wrapper = mountNoteTextContent(note)
  await wrapper.vm.$router.push({
    ...notePropertyItemLocation(note.id, "activity", focus),
    query: { conversation: "7" },
  })
  await flushPromises()
  return {
    note,
    content,
    info,
    deletion,
    save,
    route: () => wrapper.vm.$router.currentRoute.value,
  }
}
const remove = async (index: number) => {
  await wrapper
    .get(`[aria-label="Remove activity item ${index + 1}"]`)
    .trigger("click")
  await flushPromises()
}
const history = () => [
  makeMe.aMemoryTracker.id(11).withPropertyKey("activity", "run").please(),
  makeMe.aMemoryTracker.id(12).withPropertyKey("activity", "walk").please(),
  makeMe.aMemoryTracker.id(13).withPropertyKey("other", "run").please(),
  makeMe.aMemoryTracker.id(14).withPropertyKey("activity", "jog").please(),
]

it("cancels with content, history, focus and URL intact, then confirms exact history and saves siblings in order", async () => {
  const state = await mountItems()
  state.info.mockResolvedValue(wrapSdkResponse({ memoryTrackers: history() }))
  const originalRoute = state.route().fullPath
  await remove(1)
  const warning = await answerOnlyPendingPopup(false)
  expect(warning).toMatchObject({
    type: "confirm",
    message: expect.stringContaining('Removing "run"'),
  })
  expect(state.save).not.toHaveBeenCalled()
  expect(state.deletion).not.toHaveBeenCalled()
  expect(state.route().fullPath).toBe(originalRoute)
  expect(wrapper.get(selected).attributes("data-property-value")).toBe("run")
  await remove(1)
  await answerOnlyPendingPopup(true)
  await vi.waitFor(() => expect(state.save).toHaveBeenCalled())
  expect(state.deletion.mock.calls).toEqual([[{ path: { memoryTracker: 11 } }]])
  expect(state.save).toHaveBeenCalledWith({
    path: { note: state.note.id },
    body: {
      content: "---\nactivity:\n  - walk\n  - jog\nsource: local\n---\nBody.",
    },
  })
  expect(state.route()).toMatchObject({
    ...notePropertyLocation(state.note.id, "activity"),
    query: { conversation: "7" },
  })
  expect(
    wrapper.get('[data-testid="rich-note-property-panel"]').isVisible()
  ).toBe(true)
  expect(wrapper.findAll(selected)).toHaveLength(0)
})

it("explains shared equal-value history and keeps the selected surviving duplicate under occurrence one", async () => {
  const state = await mountItems(["run", "run", "walk"], {
    value: "run",
    occurrence: 2,
  })
  state.info.mockResolvedValue(wrapSdkResponse({ memoryTrackers: history() }))
  await remove(0)
  expect((await answerOnlyPendingPopup(true))?.message).toContain(
    "any remaining equal occurrence"
  )
  await vi.waitFor(() => expect(state.save).toHaveBeenCalled())
  expect(state.save.mock.calls[0]![0].body!.content).toContain(
    "activity:\n  - run\n  - walk"
  )
  expect(state.route()).toMatchObject(
    notePropertyItemLocation(state.note.id, "activity", {
      value: "run",
      occurrence: 1,
    })
  )
  expect(wrapper.get(selected).attributes("data-property-value")).toBe("run")
})

it("removes an authored blank directly while preserving a distinct selection", async () => {
  const state = await mountItems(["", "run", "walk"])
  await remove(0)
  await vi.waitFor(() => expect(state.save).toHaveBeenCalled())
  expect(state.route()).toMatchObject(
    notePropertyItemLocation(state.note.id, "activity", {
      value: "run",
      occurrence: 1,
    })
  )
  expect(pendingPopups()).toEqual([])
  expect(state.save.mock.calls[0]![0].body!.content).toContain(
    "activity:\n  - run\n  - walk"
  )
})

it.each(["load", "delete"])(
  "preserves content and focus when history %s fails with error feedback",
  async (failure) => {
    const state = await mountItems()
    state.info.mockResolvedValue(
      failure === "load"
        ? wrapSdkError({ message: "History unavailable" })
        : wrapSdkResponse({ memoryTrackers: history() })
    )
    state.deletion.mockResolvedValue(
      wrapSdkError({ message: "History unavailable" })
    )
    const originalRoute = state.route().fullPath
    await remove(1)
    if (failure === "delete") await answerOnlyPendingPopup(true)
    expect((await toastShown("error")).textContent).toContain(
      "History unavailable"
    )
    expect(state.save).not.toHaveBeenCalled()
    expect(state.route().fullPath).toBe(originalRoute)
  }
)

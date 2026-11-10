import {
  MemoryTrackerController,
  NoteController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import {
  answerOnlyPendingPopup,
  emptyPopupStack,
} from "@tests/helpers/popupStackTestSupport"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, expect, it, vi } from "vitest"
import {
  notePropertyItemLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import { mountNoteTextContent } from "./noteTextContentTestSupport"

let wrapper: VueWrapper

afterEach(() => {
  wrapper?.unmount()
  emptyPopupStack()
  vi.restoreAllMocks()
})

it("cancels then removes the final authored-key item with its whole-key history and returns to the note", async () => {
  const key = " activity "
  const note = makeMe.aNote
    .content(`---\n"${key}": [run]\nsource: local\n---\nBody.`)
    .please()
  mockSdkService(NoteController, "getNoteInfo", {
    memoryTrackers: [
      makeMe.aMemoryTracker.id(11).withPropertyKey(key, "run").please(),
      makeMe.aMemoryTracker.id(12).withPropertyKey(key, "jog").please(),
      makeMe.aMemoryTracker.id(13).withPropertyKey(key, "").please(),
      makeMe.aMemoryTracker.id(14).withPropertyKey("other", "run").please(),
    ],
  })
  const deletion = mockSdkService(MemoryTrackerController, "delete", undefined)
  const save = mockSdkServiceWithImplementation(
    TextContentController,
    "updateNoteContent",
    async (options) => makeMe.aNoteRealm.content(options.body!.content).please()
  )
  wrapper = mountNoteTextContent(note)
  await wrapper.vm.$router.push({
    ...notePropertyItemLocation(note.id, key, { value: "run", occurrence: 1 }),
    query: { conversation: "7" },
  })
  await flushPromises()
  const originalRoute = wrapper.vm.$router.currentRoute.value.fullPath
  const remove = () =>
    wrapper.get(`[aria-label="Remove ${key} item 1"]`).trigger("click")

  await remove()
  await flushPromises()
  expect(await answerOnlyPendingPopup(false)).toMatchObject({ type: "confirm" })
  expect(save).not.toHaveBeenCalled()
  expect(deletion).not.toHaveBeenCalled()
  expect(wrapper.vm.$router.currentRoute.value.fullPath).toBe(originalRoute)
  expect(
    wrapper
      .get('[data-property-value-focused="true"]')
      .attributes("data-property-value")
  ).toBe("run")

  await remove()
  await flushPromises()
  await answerOnlyPendingPopup(true)
  await vi.waitFor(() => expect(save).toHaveBeenCalled())
  expect(deletion.mock.calls).toEqual([
    [{ path: { memoryTracker: 11 } }],
    [{ path: { memoryTracker: 12 } }],
    [{ path: { memoryTracker: 13 } }],
  ])
  expect(save).toHaveBeenCalledWith({
    path: { note: note.id },
    body: { content: "---\nsource: local\n---\nBody." },
  })
  expect(wrapper.vm.$router.currentRoute.value).toMatchObject({
    ...noteShowLocation(note.id),
    query: { conversation: "7" },
  })
})

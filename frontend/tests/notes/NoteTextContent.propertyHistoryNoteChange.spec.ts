import {
  MemoryTrackerController,
  NoteController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkResponse,
} from "@tests/helpers"
import {
  answerOnlyPendingPopup,
  emptyPopupStack,
} from "@tests/helpers/popupStackTestSupport"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, expect, it, vi } from "vitest"
import { notePropertyItemLocation } from "@/routes/noteShowLocation"
import { mountNoteTextContent } from "./noteTextContentTestSupport"

let wrapper: VueWrapper
const content = "---\nactivity: [walk, run]\n---\nBody."
const history = (id: number) =>
  makeMe.aNoteRecallInfo
    .memoryTrackers([
      makeMe.aMemoryTracker.id(id).withPropertyKey("activity", "run").please(),
    ])
    .please()

afterEach(() => {
  wrapper?.unmount()
  emptyPopupStack()
  vi.restoreAllMocks()
})

async function mountFirstNote() {
  const first = makeMe.aNote.id(1).content(content).please()
  const info = mockSdkService(NoteController, "getNoteInfo", history(11))
  const deletion = mockSdkService(MemoryTrackerController, "delete", undefined)
  const save = mockSdkServiceWithImplementation(
    TextContentController,
    "updateNoteContent",
    async (options) =>
      makeMe.aNoteRealm
        .id(options.path!.note)
        .content(options.body!.content)
        .please()
  )
  wrapper = mountNoteTextContent(first)
  await wrapper.vm.$router.push(
    notePropertyItemLocation(1, "activity", { value: "run", occurrence: 1 })
  )
  await flushPromises()
  return { info, deletion, save }
}

async function switchNote() {
  await wrapper.setProps({
    note: makeMe.aNote.id(99).content(content).please(),
  })
  await wrapper.vm.$router.push(
    notePropertyItemLocation(99, "activity", { value: "run", occurrence: 1 })
  )
  await flushPromises()
}

async function removeRun() {
  await wrapper.get('[aria-label="Remove activity item 2"]').trigger("click")
  await flushPromises()
}

it("targets the current note after a canceled removal and editor reuse", async () => {
  const state = await mountFirstNote()
  await removeRun()
  await answerOnlyPendingPopup(false)
  state.info.mockResolvedValue(wrapSdkResponse(history(22)))
  await switchNote()
  await removeRun()
  await answerOnlyPendingPopup(true)
  await vi.waitFor(() => expect(state.save).toHaveBeenCalled())
  expect(state.deletion.mock.calls).toEqual([[{ path: { memoryTracker: 22 } }]])
  expect(state.save).toHaveBeenCalledWith({
    path: { note: 99 },
    body: { content: "---\nactivity:\n  - walk\n---\nBody." },
  })
})

it("keeps the current note history when a prior note's pending read completes", async () => {
  const state = await mountFirstNote()
  let resolveInfo!: (
    value: Awaited<ReturnType<typeof NoteController.getNoteInfo>>
  ) => void
  const pending = new Promise<
    Awaited<ReturnType<typeof NoteController.getNoteInfo>>
  >((resolve) => {
    resolveInfo = resolve
  })
  state.info.mockReturnValueOnce(pending)
  await removeRun()
  state.info.mockResolvedValue(wrapSdkResponse(history(22)))
  await switchNote()
  await removeRun()
  resolveInfo(wrapSdkResponse(history(11)))
  await flushPromises()
  await answerOnlyPendingPopup(false)
  await removeRun()
  await answerOnlyPendingPopup(true)
  await vi.waitFor(() => expect(state.save).toHaveBeenCalled())
  expect(state.deletion.mock.calls).toEqual([[{ path: { memoryTracker: 22 } }]])
})

it("warns and targets history added in the same note after cancellation", async () => {
  const state = await mountFirstNote()
  await removeRun()
  await answerOnlyPendingPopup(false)
  state.info.mockResolvedValue(
    wrapSdkResponse(
      makeMe.aNoteRecallInfo
        .memoryTrackers([
          makeMe.aMemoryTracker
            .id(11)
            .withPropertyKey("activity", "run")
            .please(),
          makeMe.aMemoryTracker
            .id(33)
            .withPropertyKey("activity", "walk")
            .please(),
        ])
        .please()
    )
  )
  await wrapper.get('[aria-label="Remove activity item 1"]').trigger("click")
  await flushPromises()
  expect(await answerOnlyPendingPopup(true)).toMatchObject({
    type: "confirm",
    message: expect.stringContaining('Removing "walk"'),
  })
  await vi.waitFor(() => expect(state.save).toHaveBeenCalled())
  expect(state.deletion.mock.calls).toEqual([[{ path: { memoryTracker: 33 } }]])
})

import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { afterEach, expect, it } from "vitest"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { atViewport } from "./propertyLayoutTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"
import { mountEditorOnNoteShow } from "./propertyValueDialogTestDom"
import { expandPropertyPanel, propertyRowSelector } from "./propertiesTestDom"

const h = createRichMarkdownEditorTestHarness()
afterEach(() => h.cleanup())
it("keeps narrow touch selection and tracker hit targets stable while tracker information loads", async () => {
  await atViewport(320)
  expect(matchMedia("(pointer: coarse)").matches).toBe(true)
  let resolveInfo!: (
    value: Awaited<ReturnType<typeof NoteController.getNoteInfo>>
  ) => void
  const pending = new Promise<
    Awaited<ReturnType<typeof NoteController.getNoteInfo>>
  >((resolve) => {
    resolveInfo = resolve
  })
  mockSdkService(NoteController, "getNoteInfo", {
    memoryTrackers: [],
  }).mockReturnValue(pending)
  const wrapper = await mountEditorOnNoteShow(
    h,
    "---\nactivity: [run, walk]\n---\nBody.",
    { route: noteShowLocation(42) }
  )
  await expandPropertyPanel(wrapper, propertyRowSelector("activity"))
  const row = wrapper.get('[data-property-value="run"]').element as HTMLElement
  const before = row.getBoundingClientRect()
  const select = row.querySelector(".property-item-select")!
  expect(select.getBoundingClientRect().height).toBeGreaterThanOrEqual(44)
  resolveInfo(
    wrapSdkResponse({
      memoryTrackers: [
        makeMe.aMemoryTracker
          .id(77)
          .withPropertyKey("activity", "run")
          .please(),
      ],
    })
  )
  await flushPromises()
  const tracker = row.querySelector(
    '[data-test="assimilation-status-UNDERSTANDING"]'
  ) as HTMLElement
  expect(tracker.getBoundingClientRect().height).toBeGreaterThanOrEqual(44)
  expect(row.getBoundingClientRect().height).toBe(before.height)
  const box = tracker.getBoundingClientRect()
  expect(
    document
      .elementFromPoint(box.left + box.width / 2, box.top + box.height / 2)
      ?.closest("a")
  ).toBe(tracker)
  tracker.click()
  await flushPromises()
  expect(wrapper.vm.$router.currentRoute.value).toMatchObject({
    name: "memoryTrackerShow",
    params: { memoryTrackerId: "77" },
  })
})

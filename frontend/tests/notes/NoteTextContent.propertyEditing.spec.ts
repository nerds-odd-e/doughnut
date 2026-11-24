import {
  MemoryTrackerController,
  NoteController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import { cleanup, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  productionRouterAt,
} from "@tests/helpers"
import {
  installMockResizeObserver,
  restoreNoteToolbarWidthMocks,
} from "@tests/helpers/mockNoteToolbarNavWidth"
import {
  renderNoteShowPageWithoutSidebar,
  setupNoteShowPageMocks,
} from "../pages/noteShowPageTestSupport"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { useContentCursorInserter } from "@/composables/useContentCursorInserter"

beforeEach(() => installMockResizeObserver())
afterEach(() => {
  cleanup()
  document.body.innerHTML = ""
  restoreNoteToolbarWidthMocks()
})

async function showNote(content = "Body.") {
  const realm = setupNoteShowPageMocks(
    makeMe.aNoteRealm.content(content).please()
  )
  const save = mockSdkServiceWithImplementation(
    TextContentController,
    "updateNoteContent",
    async ({ body }) =>
      makeMe.aNoteRealm
        .id(realm.id)
        .content(body?.content ?? "")
        .please()
  )
  await renderNoteShowPageWithoutSidebar(
    await productionRouterAt(noteShowLocation(realm.id)),
    realm.id
  )
  return save
}

function propertyRow(key: string) {
  return screen
    .getAllByTestId("rich-note-property-row")
    .find((row) => row.dataset.propertyKey === key)!
}

async function nameProperty(oldKey: string, newKey: string) {
  const input = propertyRow(oldKey).querySelector<HTMLInputElement>(
    '[data-testid="rich-note-property-row-key-input"]'
  )!
  input.focus()
  input.value = newKey
  input.dispatchEvent(new Event("input", { bubbles: true }))
  await flushPromises()
  input.blur()
  await flushPromises()
}

it("keeps an unfinished draft with its validation, then saves the corrected property", async () => {
  const save = await showNote()
  screen.getByText("Add property").click()
  await flushPromises()
  const key = screen.getByLabelText("Property key") as HTMLInputElement
  key.value = "topic"
  key.dispatchEvent(new Event("input", { bubbles: true }))
  await flushPromises()
  screen.getByTestId("rich-note-property-row-add").click()
  await flushPromises()
  expect(screen.getByText("Enter a property value.").checkVisibility()).toBe(
    true
  )

  expect(
    (screen.getByLabelText("Property key") as HTMLInputElement).value
  ).toBe("topic")
  expect(screen.getByText("Enter a property value.").checkVisibility()).toBe(
    true
  )
  const value = screen.getByLabelText("Property value")
  expect(value.textContent).toBe("")
  value.textContent = "training"
  value.dispatchEvent(new Event("input", { bubbles: true }))
  await flushPromises()
  screen.getByTestId("rich-note-property-row-add").click()
  await flushPromises()
  ;(document.querySelector(".ql-editor") as HTMLElement).dispatchEvent(
    new Event("blur")
  )
  await flushPromises()
  expect(save).toHaveBeenCalledWith(
    expect.objectContaining({
      body: { content: "---\ntopic: training\n---\nBody." },
    })
  )
  const row = screen.getByTestId("rich-note-property-row")
  expect(row.checkVisibility()).toBe(true)
  expect(row.textContent).toContain("training")
})

it.each([false, true])(
  "inserts a wiki link as a property (Markdown: %s)",
  async (asMarkdown) => {
    const save = await showNote()
    if (asMarkdown) {
      screen.getByTitle("Edit as markdown (m)").click()
      await flushPromises()
    }
    useContentCursorInserter().insertWikiLinkAsProperty("[[Target]]")
    await flushPromises()
    expect(save).toHaveBeenCalledWith(
      expect.objectContaining({
        body: { content: '---\n"": "[[Target]]"\n---\nBody.' },
      })
    )
    if (asMarkdown) {
      expect(
        (
          document.querySelector(
            '[aria-label="Note content"] textarea'
          ) as HTMLTextAreaElement
        ).value
      ).toContain('"": "[[Target]]"')
      screen.getByTitle("Edit as rich content (m)").click()
      await flushPromises()
    }
    const row = screen.getByTestId("rich-note-property-row")
    expect(row.checkVisibility()).toBe(true)
    expect(row.textContent).toContain("Target")
  }
)

it("names a picked link using a preset that creates its first value", async () => {
  const save = await showNote()
  useContentCursorInserter().insertWikiLinkAsProperty("[[Swim]]")
  await flushPromises()
  screen.getByLabelText("Existing note property key (row 1)").focus()
  await flushPromises()
  screen.getByText("example of", { selector: "button" }).click()
  await flushPromises()

  expect(save.mock.calls.map(([options]) => options.body?.content)).toEqual([
    '---\n"": "[[Swim]]"\n---\nBody.',
    '---\nexample of: "[[Swim]]"\n---\nBody.',
  ])
})

it("names a picked link using the exact authored key and appends to its scalar", async () => {
  mockSdkService(NoteController, "getNoteInfo", makeMe.aNoteRecallInfo.please())
  mockSdkService(MemoryTrackerController, "followPropertyValue", undefined)
  const save = await showNote(
    '---\nrelated topics: "[[Strength]]"\nRelated topics: "[[Fitness]]"\n---\nBody.'
  )
  useContentCursorInserter().insertWikiLinkAsProperty("[[Mobility]]")
  await flushPromises()
  await nameProperty("", " related topics ")

  expect(save).toHaveBeenCalledTimes(2)
  expect(save).toHaveBeenLastCalledWith(
    expect.objectContaining({
      body: {
        content:
          '---\nrelated topics:\n  - "[[Strength]]"\n  - "[[Mobility]]"\nRelated topics: "[[Fitness]]"\n---\nBody.',
      },
    })
  )
})

it("keeps a picked link available for a corrected name after a scalar-only collision", async () => {
  const save = await showNote("---\nimage: /cover.png\n---\nBody.")
  useContentCursorInserter().insertWikiLinkAsProperty("[[Swim]]")
  await flushPromises()
  await nameProperty("", "image")

  expect(
    screen
      .getByText("Duplicate property keys are not allowed.")
      .checkVisibility()
  ).toBe(true)
  expect(propertyRow("").textContent).toContain("Swim")
  expect(
    propertyRow("image").querySelector<HTMLInputElement>(
      '[data-testid="rich-note-property-row-value-input"]'
    )?.value
  ).toBe("/cover.png")
  expect(save).toHaveBeenCalledOnce()

  await nameProperty("", "example of")
  expect(save).toHaveBeenCalledTimes(2)
  expect(save).toHaveBeenLastCalledWith(
    expect.objectContaining({
      body: {
        content: '---\nimage: /cover.png\nexample of: "[[Swim]]"\n---\nBody.',
      },
    })
  )
})

it("preserves both named properties when a rename would collide with a list-capable key", async () => {
  const save = await showNote(
    "---\nrelated topics: Strength\ntopic: Mobility\n---\nBody."
  )
  await nameProperty("topic", "related topics")

  expect(
    screen
      .getByText("Duplicate property keys are not allowed.")
      .checkVisibility()
  ).toBe(true)
  expect(propertyRow("related topics").textContent).toContain("Strength")
  expect(propertyRow("topic").textContent).toContain("Mobility")
  expect(save).not.toHaveBeenCalled()
})

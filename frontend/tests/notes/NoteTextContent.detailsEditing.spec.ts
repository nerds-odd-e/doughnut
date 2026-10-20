import { TextContentController } from "@generated/donut-backend-api/sdk.gen"
import { cleanup, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import {
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

async function showNote() {
  const realm = setupNoteShowPageMocks(
    makeMe.aNoteRealm.content("Body.").please()
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

async function toggleDetails() {
  screen.getByTestId("note-details-toggle").click()
  await flushPromises()
}

it("retains an unfinished draft and validation through collapse, then saves its summary", async () => {
  const save = await showNote()
  await toggleDetails()
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

  await toggleDetails()
  expect(
    screen.getByTestId("note-details-unfinished-edit").checkVisibility()
  ).toBe(true)
  await toggleDetails()
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
  await toggleDetails()
  expect(screen.getByTestId("note-property-summary").textContent).toBe(
    "Properties: topic"
  )
})

it.each([false, true])(
  "reveals property insertion from a compact note (Markdown: %s)",
  async (asMarkdown) => {
    const save = await showNote()
    if (asMarkdown) {
      screen.getByTitle("Edit as markdown (m)").click()
      await flushPromises()
    }
    useContentCursorInserter().insertWikiLinkAsProperty("[[Target]]")
    await flushPromises()
    expect(
      screen.getByTestId("note-details-toggle").getAttribute("aria-expanded")
    ).toBe("true")
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

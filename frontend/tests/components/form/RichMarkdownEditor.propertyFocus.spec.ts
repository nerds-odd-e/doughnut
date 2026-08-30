import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService } from "@tests/helpers"
import { notePropertyLocation } from "@/routes/noteShowLocation"
import {
  expectPropertyPanelClosed,
  expectPropertyPanelOpen,
  propertyRowSelector,
} from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property focus from noteProperty", () => {
  const h = createRichMarkdownEditorTestHarness()
  const noteId = 42
  const markdown = `---
diligence: high
topic: training
---

Workshop body.`

  beforeEach(() => {
    mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  it("visiting noteProperty focuses the row, scrolls it into view, and opens its property panel", async () => {
    const scrollSpy = vi.spyOn(HTMLElement.prototype, "scrollIntoView")

    const wrapper = await h.mountEditor(markdown, {
      attachToBody: true,
      noteId,
      route: notePropertyLocation(noteId, "topic"),
    })

    const topicRow = wrapper.find(propertyRowSelector("topic"))
    expect(topicRow.attributes("data-property-focused")).toBe("true")
    expect(topicRow.classes()).toContain("bg-primary/10")
    expectPropertyPanelOpen(topicRow.element)
    expect(
      wrapper
        .find(propertyRowSelector("diligence"))
        .attributes("data-property-focused")
    ).toBeUndefined()
    expectPropertyPanelClosed(
      wrapper.find(propertyRowSelector("diligence")).element
    )
    expect(scrollSpy).toHaveBeenCalledWith({
      behavior: "smooth",
      block: "center",
    })
  })

  it("visiting noteProperty on a read-only property focuses the row, scrolls it into view, and shows the value", async () => {
    const scrollSpy = vi.spyOn(HTMLElement.prototype, "scrollIntoView")

    const wrapper = await h.mountEditor(markdown, {
      attachToBody: true,
      noteId,
      readonly: true,
      route: notePropertyLocation(noteId, "topic"),
    })

    const topicRow = wrapper.find(propertyRowSelector("topic"))
    expect(topicRow.attributes("data-property-focused")).toBe("true")
    expect(topicRow.classes()).toContain("bg-primary/10")
    expect(topicRow.text()).toContain("training")
    expect(
      wrapper
        .find(propertyRowSelector("diligence"))
        .attributes("data-property-focused")
    ).toBeUndefined()
    expect(scrollSpy).toHaveBeenCalledWith({
      behavior: "smooth",
      block: "center",
    })
  })

  it("visiting noteProperty on a specialized property focuses the row and opens its property panel", async () => {
    const specializedMarkdown = `---
image: https://example.com/workshop.png
topic: training
---

Workshop body.`

    const wrapper = await h.mountEditor(specializedMarkdown, {
      attachToBody: true,
      noteId,
      route: notePropertyLocation(noteId, "image"),
    })

    const imageRow = wrapper.find(propertyRowSelector("image"))
    expect(imageRow.attributes("data-property-focused")).toBe("true")
    expectPropertyPanelOpen(imageRow.element)
    expect(
      imageRow.find('[data-testid="rich-note-image-property-choose"]').exists()
    ).toBe(true)
  })

  it("visiting noteProperty for a missing key shows not-found with the decoded key", async () => {
    const wrapper = await h.mountEditor(markdown, {
      attachToBody: true,
      noteId,
      route: notePropertyLocation(noteId, "example of"),
    })

    expect(
      wrapper.find('[data-testid="rich-note-property-not-found"]').text()
    ).toBe('Property "example of" not found')
    expect(
      wrapper
        .find(propertyRowSelector("topic"))
        .attributes("data-property-focused")
    ).toBeUndefined()
  })
})

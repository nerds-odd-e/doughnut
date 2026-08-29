import { afterEach, describe, expect, it, vi } from "vitest"
import { notePropertyLocation } from "@/routes/noteShowLocation"
import { dialogEl } from "./propertyValuePopupTestDom"
import { propertyRowSelector } from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property focus from noteProperty", () => {
  const h = createRichMarkdownEditorTestHarness()
  const noteId = 42
  const markdown = `---
diligence: high
topic: training
---

Workshop body.`

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  it("visiting noteProperty focuses the row, scrolls it into view, and opens its value dialog", async () => {
    const scrollSpy = vi.spyOn(HTMLElement.prototype, "scrollIntoView")

    const wrapper = await h.mountEditor(markdown, {
      attachToBody: true,
      noteId,
      route: notePropertyLocation(noteId, "topic"),
    })

    const topicRow = wrapper.find(propertyRowSelector("topic"))
    expect(topicRow.attributes("data-property-focused")).toBe("true")
    expect(topicRow.classes()).toContain("bg-primary/10")
    expect(
      wrapper
        .find(propertyRowSelector("diligence"))
        .attributes("data-property-focused")
    ).toBeUndefined()
    expect(dialogEl()).not.toBeNull()
    expect(document.querySelector("dialog h2")?.textContent).toBe("topic")
    expect(scrollSpy).toHaveBeenCalledWith({
      behavior: "smooth",
      block: "center",
    })
  })
})

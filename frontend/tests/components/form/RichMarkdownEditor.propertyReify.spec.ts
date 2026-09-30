import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { expandPropertyPanel, propertyRowSelector } from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property reify", () => {
  const h = createRichMarkdownEditorTestHarness()
  const noteId = 42
  const markdown = `---
related: "[[Other]]"
---

Body`
  const relatedRowSelector = propertyRowSelector("related")
  const reifyButton = `${relatedRowSelector} [data-testid="rich-note-property-row-reify"]`

  beforeEach(() => {
    mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  it("reifies a wiki-link property and shows the new relationship note", async () => {
    const relationshipRealm = makeMe.aNoteRealm.id(77).please()
    const reifySpy = mockSdkService(
      NoteController,
      "reifyProperty",
      relationshipRealm
    )
    mockSdkService(
      NoteController,
      "showNote",
      makeMe.aNoteRealm.id(noteId).please()
    )
    const wrapper = await h.mountEditor(markdown, {
      noteId,
      route: noteShowLocation(noteId),
      wikiLinks: [wikiLinkFromAuthoredToken("Other", 5)],
    })
    await expandPropertyPanel(wrapper, relatedRowSelector)

    await wrapper.find(reifyButton).trigger("click")
    await flushPromises()

    expect(reifySpy).toHaveBeenCalledWith({
      path: { note: noteId },
      query: { propertyKey: "related" },
    })
    expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
      noteShowLocation(77)
    )
  })

  const notLinkReason =
    "Only a property whose value is a link to a note can be reified"

  it.each([
    ["a plain-text value", "kind", "kind: training", notLinkReason],
    [
      "a list value",
      "tags",
      'tags:\n  - "[[Other]]"\n  - plain',
      notLinkReason,
    ],
    [
      "a structural property",
      "source",
      'type: Relationship\nsource: "[[Src]]"\ntarget: "[[Other]]"',
      "A structural property cannot be reified",
    ],
  ])(
    "explains why %s cannot be reified",
    async (_, key, frontmatter, reason) => {
      const reifySpy = mockSdkService(
        NoteController,
        "reifyProperty",
        makeMe.aNoteRealm.please()
      )
      const rowSelector = propertyRowSelector(key)
      const wrapper = await h.mountEditor(`---\n${frontmatter}\n---\n\nBody`, {
        noteId,
        route: noteShowLocation(noteId),
        wikiLinks: [
          wikiLinkFromAuthoredToken("Other", 5),
          wikiLinkFromAuthoredToken("Src", 6),
        ],
      })
      await expandPropertyPanel(wrapper, rowSelector)
      const button = wrapper.find(
        `${rowSelector} [data-testid="rich-note-property-row-reify"]`
      )

      expect(button.attributes("disabled")).toBeDefined()
      expect(wrapper.find(rowSelector).text()).toContain(reason)
      await button.trigger("click")
      await flushPromises()
      expect(reifySpy).not.toHaveBeenCalled()
    }
  )

  it("offers no reify in the read-only view", async () => {
    const wrapper = await h.mountEditor(markdown, {
      noteId,
      readonly: true,
      lastSavedMarkdown: markdown,
      wikiLinks: [wikiLinkFromAuthoredToken("Other", 5)],
    })

    expect(wrapper.find(reifyButton).exists()).toBe(false)
  })
})

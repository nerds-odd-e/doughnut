import { flushPromises } from "@vue/test-utils"
import { relationshipNoteContent } from "@tests/notes/relationshipNoteTestContent"
import { wikiTitleFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { propertyRowSelector } from "./propertiesTestDom"
import {
  clickDeadWikiLinkInPropertyValue,
  DEAD_LINK_CLICK_CASES,
  propertyWikiLinkMarkdown,
} from "./propertiesTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property wiki links", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it.each(DEAD_LINK_CLICK_CASES)(
    "emits deadWikiLinkClick for property wiki link ($case)",
    async ({ wikiToken, expected }) => {
      const wrapper = await h.mountEditor(propertyWikiLinkMarkdown(wikiToken))
      await clickDeadWikiLinkInPropertyValue(wrapper)
      expect(wrapper.emitted("deadWikiLinkClick")?.[0]).toEqual([expected])
    }
  )

  it("shows an unconfirmed property wiki link as pending then dead when last-saved catches up", async () => {
    const saved = `---
topic: old
---

Body`
    const inFlight = propertyWikiLinkMarkdown("WikiLinks E2E Nowhere")
    const wrapper = await h.mountEditor(inFlight, {
      lastSavedMarkdown: saved,
      wikiTitles: [],
    })
    const valueField = () =>
      wrapper.find('[data-testid="rich-note-property-row-value-input"]')

    expect(valueField().find("a.pending-wiki-link").exists()).toBe(true)
    expect(
      valueField().find("a.pending-wiki-link").attributes("data-wiki-title")
    ).toBe("WikiLinks E2E Nowhere")

    await wrapper.setProps({ lastSavedMarkdown: inFlight, wikiTitles: [] })
    await flushPromises()

    expect(valueField().find("a.pending-wiki-link").exists()).toBe(false)
    expect(
      valueField().find("a.dead-wiki-link").attributes("data-wiki-title")
    ).toBe("WikiLinks E2E Nowhere")
  })

  it("shows a property wiki link as live when wikiTitles resolve it", async () => {
    const markdown = propertyWikiLinkMarkdown("My Note")
    const wrapper = await h.mountEditor(markdown, {
      lastSavedMarkdown: markdown,
      wikiTitles: [wikiTitleFromAuthoredToken("My Note", 42)],
    })
    const live = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"] a.donut-wiki-link'
    )
    expect(live.exists()).toBe(true)
  })

  it("shows path Markdown in a relationship source as a live wiki-style link", async () => {
    const wrapper = await h.mountEditor(
      relationshipNoteContent("a-part-of", "[Moon](/Moon.md)", "[[Earth]]"),
      {
        wikiTitles: [wikiTitleFromAuthoredToken("[Moon](/Moon.md)", 42)],
      }
    )
    const live = wrapper
      .find(propertyRowSelector("source"))
      .element.querySelector("a.donut-wiki-link") as HTMLAnchorElement
    expect(live.getAttribute("href")).toBe(noteShowHref(42))
    expect(live.textContent).toBe("Moon")
    expect(live.querySelector(".wiki-bracket")).toBeNull()
  })
})

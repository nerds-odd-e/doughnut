import { flushPromises } from "@vue/test-utils"
import {
  notePropertyHref,
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { vi } from "vitest"
import {
  clickDeadWikiLinkInPropertyValue,
  DEAD_LINK_CLICK_CASES,
  propertyWikiLinkMarkdown,
} from "./propertiesTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property wiki links", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    vi.restoreAllMocks()
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
      wikiLinks: [],
    })
    const valueField = () =>
      wrapper.find('[data-testid="rich-note-property-row-value-input"]')

    expect(valueField().find("a.pending-wiki-link").exists()).toBe(true)
    expect(
      valueField().find("a.pending-wiki-link").attributes("data-portable-path")
    ).toBe("WikiLinks E2E Nowhere")

    await wrapper.setProps({ lastSavedMarkdown: inFlight, wikiLinks: [] })
    await flushPromises()

    expect(valueField().find("a.pending-wiki-link").exists()).toBe(false)
    expect(
      valueField().find("a.dead-wiki-link").attributes("data-portable-path")
    ).toBe("WikiLinks E2E Nowhere")
  })

  it("clicking a resolved property wiki in a property value pushes noteProperty and does not rewrite on blur", async () => {
    const token = "Moon#prop:a%20part%20of"
    const markdown = propertyWikiLinkMarkdown(token)
    const wrapper = await h.mountEditor(markdown, {
      lastSavedMarkdown: markdown,
      wikiLinks: [wikiLinkFromAuthoredToken(token, 42)],
      route: noteShowLocation(99),
      noteId: 99,
    })
    const valueField = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"]'
    )
    const live = valueField.find("a.donut-wiki-link")
    expect(live.attributes("href")).toBe(notePropertyHref(42, "a part of"))
    const updateCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0

    ;(live.element as HTMLAnchorElement).click()
    await flushPromises()
    await valueField.trigger("blur")

    expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
      notePropertyLocation(42, "a part of")
    )
    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      updateCountBefore
    )
  })

  it("shows a property wiki link as live when wikiLinks resolve it", async () => {
    const markdown = propertyWikiLinkMarkdown("My Note")
    const wrapper = await h.mountEditor(markdown, {
      lastSavedMarkdown: markdown,
      wikiLinks: [wikiLinkFromAuthoredToken("My Note", 42)],
    })
    const live = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"] a.donut-wiki-link'
    )
    expect(live.exists()).toBe(true)
  })
})

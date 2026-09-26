import { flushPromises } from "@vue/test-utils"
import {
  notePropertyHref,
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { vi } from "vitest"
import {
  deadWikiLinkInPropertyValueEl,
  propertyRowListValue,
} from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

function propertyWikiLinkMarkdown(wikiToken: string): string {
  return `---
topic: "[[${wikiToken}]]"
---

Body`
}

describe("RichMarkdownEditor property wiki links", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
  })

  it.each([
    {
      case: "plain wiki token",
      wikiToken: "Missing Note",
      expected: { portablePath: "Missing Note", displayText: "Missing Note" },
    },
    {
      case: "display text",
      wikiToken: "Ghost Page|shown text",
      expected: { portablePath: "Ghost Page", displayText: "shown text" },
    },
  ])(
    "emits deadWikiLinkClick for property wiki link ($case)",
    async ({ wikiToken, expected }) => {
      const wrapper = await h.mountEditor(propertyWikiLinkMarkdown(wikiToken))
      deadWikiLinkInPropertyValueEl(wrapper.element).click()
      await flushPromises()
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

  it.each([
    `---\ntopic: '[[A\\|B|C\\|D]]'\n---\n\nBody`,
    `---\ntopic: "[[A\\\\|B|C\\\\|D]]"\n---\n\nBody`,
  ])("renders equivalent quoted YAML wiki escapes", async (markdown) => {
    const wrapper = await h.mountEditor(markdown, {
      lastSavedMarkdown: markdown,
      wikiLinks: [wikiLinkFromAuthoredToken("A\\|B|C\\|D", 42)],
    })

    const live = wrapper.get(
      '[data-testid="rich-note-property-row-value-input"] a.donut-wiki-link'
    )
    expect(live.text()).toContain("C|D")
  })

  it("renders resolved and dead overlaps list items, marking new links pending until saved", async () => {
    const inFlight = `---
overlaps:
  - "[[Other Note]]"
  - "[[Missing Note]]"
  - "[[WikiLinks E2E Nowhere]]"
---

Body`
    const wrapper = await h.mountEditor(inFlight, {
      lastSavedMarkdown: `---\noverlaps:\n  - "[[Other Note]]"\n  - "[[Missing Note]]"\n---\n\nBody`,
      wikiLinks: [wikiLinkFromAuthoredToken("Other Note", 42)],
    })

    const list = propertyRowListValue(wrapper, "overlaps")
    const resolvedLink = list.find("a.router-link")
    expect(resolvedLink.text()).toBe("Other Note")
    expect(JSON.parse(resolvedLink.attributes("to") ?? "{}")).toEqual(
      noteShowLocation(42)
    )
    expect(list.text()).not.toContain("[[")
    expect(list.find("a.dead-wiki-link").text()).toBe("Missing Note")
    expect(list.find("a.pending-wiki-link").text()).toBe(
      "WikiLinks E2E Nowhere"
    )

    await wrapper.setProps({ lastSavedMarkdown: inFlight })
    await flushPromises()

    expect(list.find("a.pending-wiki-link").exists()).toBe(false)
    const deadTitles = list.findAll("a.dead-wiki-link").map((a) => a.text())
    expect(deadTitles).toEqual(["Missing Note", "WikiLinks E2E Nowhere"])
  })
})

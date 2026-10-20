import { flushPromises } from "@vue/test-utils"
import {
  notePropertyHref,
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { vi } from "vitest"
import { userEvent } from "vitest/browser"
import {
  collapsePropertyPanel,
  expandPropertyPanel,
  expectPropertyPanelClosed,
  expectPropertyPanelOpen,
  propertyRowListValue,
  propertyRowSelector,
} from "./propertiesTestDom"
import {
  clickCancel,
  propertyValueDialogEl,
} from "./propertyValueDialogTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property list wiki links", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
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

  it.each([
    { title: "Grammar", noteId: 42 },
    { title: "Syntax", noteId: 43 },
  ])(
    "follows $title independently from a mixed list",
    async ({ title, noteId }) => {
      const markdown =
        '---\ntopics: ["[[Grammar]]", "practice", "[[Syntax]]"]\n---\n\nBody'
      const wrapper = await h.mountEditor(markdown, {
        lastSavedMarkdown: markdown,
        wikiLinks: [
          wikiLinkFromAuthoredToken("Grammar", 42),
          wikiLinkFromAuthoredToken("Syntax", 43),
        ],
        route: noteShowLocation(99),
        noteId: 99,
      })
      const list = propertyRowListValue(wrapper, "topics")
      if (title === "Grammar")
        expect(list.text()).toBe("Grammar, practice, Syntax")
      const link = list
        .findAll("a.donut-wiki-link")
        .find((a) => a.text() === title)!
      ;(link.element as HTMLAnchorElement).click()
      await flushPromises()
      expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
        noteShowLocation(noteId)
      )
    }
  )

  it("follows a labeled list property target while its source panel is expanded", async () => {
    const token = "Moon#prop:a%20part%20of|lunar topic"
    const markdown = `---\ntopics: ["[[${token}]]", "practice"]\n---\n\nBody`
    const wrapper = await h.mountEditor(markdown, {
      lastSavedMarkdown: markdown,
      wikiLinks: [wikiLinkFromAuthoredToken(token, 42)],
      route: noteShowLocation(99),
      noteId: 99,
    })
    await expandPropertyPanel(wrapper, propertyRowSelector("topics"))
    expectPropertyPanelOpen(wrapper.get(propertyRowSelector("topics")).element)
    const link = wrapper.get(
      `${propertyRowSelector("topics")} [data-testid="rich-note-property-panel"] a.donut-wiki-link`
    )
    expect(link.text()).toBe("lunar topic")
    expect(link.attributes("href")).toBe(notePropertyHref(42, "a part of"))
    ;(link.element as HTMLAnchorElement).click()
    await flushPromises()
    expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
      notePropertyLocation(42, "a part of")
    )
  })

  it("switches between detailed and compact values, keeping Edit usable while expanded", async () => {
    const markdown = '---\ntopics: ["[[Grammar]]", "practice"]\n---\n\nBody'
    const wrapper = await h.mountEditor(markdown, {
      lastSavedMarkdown: markdown,
      wikiLinks: [wikiLinkFromAuthoredToken("Grammar", 42)],
      route: noteShowLocation(99),
      noteId: 99,
    })
    const rowSelector = propertyRowSelector("topics")
    await expandPropertyPanel(wrapper, rowSelector)
    const panel = `${rowSelector} [data-testid="rich-note-property-panel"]`
    expect(
      wrapper
        .findAll(`${panel} [data-property-value]`)
        .map((row) =>
          row.find('[data-test="mode-label-UNDERSTANDING"]').text().trim()
        )
    ).toEqual(["Grammar", "practice"])

    await wrapper
      .find(
        `${rowSelector} [data-testid="rich-note-property-value-dialog-open"]`
      )
      .trigger("click")
    await flushPromises()
    expect(propertyValueDialogEl()).not.toBeNull()
    clickCancel()
    await flushPromises()
    expect(propertyValueDialogEl()).toBeNull()
    await collapsePropertyPanel(wrapper, rowSelector)
    expectPropertyPanelClosed(wrapper.get(rowSelector).element)
    const link = propertyRowListValue(wrapper, "topics")
      .findAll("a.donut-wiki-link")
      .find((a) => a.text() === "Grammar")!
    ;(link.element as HTMLAnchorElement).click()
    await flushPromises()
    expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
      noteShowLocation(42)
    )
  })

  it("activates an expanded list wiki link with the keyboard", async () => {
    const markdown = '---\ntopics: ["[[Grammar]]", "practice"]\n---\n\nBody'
    const wrapper = await h.mountEditor(markdown, {
      lastSavedMarkdown: markdown,
      wikiLinks: [wikiLinkFromAuthoredToken("Grammar", 42)],
      route: noteShowLocation(99),
      noteId: 99,
      attachToBody: true,
    })
    await expandPropertyPanel(wrapper, propertyRowSelector("topics"))
    const link = wrapper.get(
      `${propertyRowSelector("topics")} [data-testid="rich-note-property-panel"] a.donut-wiki-link`
    ).element as HTMLAnchorElement
    expect(link.textContent).toBe("Grammar")
    link.focus()
    expect(document.activeElement).toBe(link)
    await userEvent.keyboard("{Enter}")
    await flushPromises()
    expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
      noteShowLocation(42)
    )
  })

  it("keeps pending expanded links inert and forwards dead expanded recovery", async () => {
    const markdown =
      '---\ntopics: ["[[Missing|shown]]", "[[New]]"]\n---\n\nBody'
    const wrapper = await h.mountEditor(markdown, {
      lastSavedMarkdown: '---\ntopics: ["[[Missing|shown]]"]\n---\n\nBody',
      route: notePropertyLocation(99, "topics"),
      noteId: 99,
    })
    await expandPropertyPanel(wrapper, propertyRowSelector("topics"))
    const panel = `${propertyRowSelector("topics")} [data-testid="rich-note-property-panel"]`
    await wrapper.get(`${panel} a.pending-wiki-link`).trigger("click")
    expect(wrapper.emitted("deadWikiLinkClick")).toBeUndefined()
    expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
      notePropertyLocation(99, "topics")
    )
    await wrapper.get(`${panel} a.dead-wiki-link`).trigger("click")
    expect(wrapper.emitted("deadWikiLinkClick")?.[0]).toEqual([
      { portablePath: "Missing", displayText: "shown" },
    ])
    expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
      notePropertyLocation(99, "topics")
    )
  })

  it("activates a readonly list wiki link with the keyboard", async () => {
    const markdown = '---\ntopics: ["[[Grammar]]", "practice"]\n---\n\nBody'
    const wrapper = await h.mountEditor(markdown, {
      readonly: true,
      attachToBody: true,
      lastSavedMarkdown: markdown,
      wikiLinks: [wikiLinkFromAuthoredToken("Grammar", 42)],
      route: noteShowLocation(99),
    })
    const link = wrapper.get("dl a.donut-wiki-link")
      .element as HTMLAnchorElement
    link.focus()
    expect(document.activeElement).toBe(link)
    await userEvent.keyboard("{Enter}")
    await flushPromises()
    expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
      noteShowLocation(42)
    )
  })

  it("forwards dead list recovery clicks but leaves pending links inert", async () => {
    const markdown =
      '---\ntopics: ["[[Missing|shown]]", "[[New]]"]\n---\n\nBody'
    const wrapper = await h.mountEditor(markdown, {
      lastSavedMarkdown: '---\ntopics: ["[[Missing|shown]]"]\n---\n\nBody',
      route: noteShowLocation(99),
      noteId: 99,
    })
    const list = propertyRowListValue(wrapper, "topics")
    await list.get("a.pending-wiki-link").trigger("click")
    expect(wrapper.emitted("deadWikiLinkClick")).toBeUndefined()
    await list.get("a.dead-wiki-link").trigger("click")
    expect(wrapper.emitted("deadWikiLinkClick")?.[0]).toEqual([
      { portablePath: "Missing", displayText: "shown" },
    ])
    expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
      noteShowLocation(99)
    )
    await wrapper.setProps({ lastSavedMarkdown: markdown })
    expect(list.findAll("a.dead-wiki-link")).toHaveLength(2)
  })
})

import { describe, expect, it } from "vitest"
import { noteShowHref } from "@/routes/noteShowLocation"
import {
  propertyValuePlainToDisplayHtml,
  serializePropertyValueFieldRoot,
} from "@/utils/propertyValueField"
import {
  deadWikiLinkPayloadFromAnchor,
  escapeHtmlForWikiLinkDisplay,
  wikiTitleFromAuthoredToken,
} from "@/utils/wikiLinkMarkup"

describe("propertyValueField utils", () => {
  it("renders unresolved wiki link with display text after pipe", () => {
    const html = propertyValuePlainToDisplayHtml(
      "See [[Unknown Topic|friendly label]] here",
      []
    )
    expect(html).toContain('class="dead-wiki-link"')
    expect(html).toContain('data-wiki-title="Unknown Topic"')
    expect(html).toContain('data-wiki-display="friendly label"')
    expect(html).toContain("friendly label")
    expect(html).not.toContain("Unknown Topic|friendly label")
  })

  it("round-trips display text wiki link from a field root", () => {
    const root = document.createElement("div")
    root.innerHTML = propertyValuePlainToDisplayHtml(
      "[[Target Page|alias text]]",
      []
    )
    expect(serializePropertyValueFieldRoot(root)).toBe(
      "[[Target Page|alias text]]"
    )
  })

  it("deadWikiLinkPayloadFromAnchor prefers data-wiki-title for piped links", () => {
    const wrap = document.createElement("div")
    wrap.innerHTML = propertyValuePlainToDisplayHtml("[[Missing|Shown]]", [])
    const a = wrap.querySelector("a.dead-wiki-link") as HTMLAnchorElement
    expect(deadWikiLinkPayloadFromAnchor(a).targetToken).toBe("Missing")
  })

  it("turns only well-formed wiki markers into dead-wiki-link anchors with visible brackets", () => {
    const html = propertyValuePlainToDisplayHtml("See [[X]] here", [])
    expect(html).toContain('class="dead-wiki-link"')
    expect(html).toContain('class="wiki-bracket"')
    expect(html).toContain("data-wiki-title")
    expect(html).toContain("X")
    expect(html).toContain("See ")
  })

  it("does not treat empty or whitespace-only brackets as a wiki link", () => {
    const html = propertyValuePlainToDisplayHtml("A [[ ]] B [[  ]]", [])
    expect(html).not.toContain("dead-wiki-link")
    expect(html).toContain("[[ ]]")
  })

  it("renders resolved path Markdown in a scalar as a live wiki-style link", () => {
    const html = propertyValuePlainToDisplayHtml("[Moon](/Moon.md)", [
      wikiTitleFromAuthoredToken("[Moon](/Moon.md)", 42),
    ])
    expect(html).toBe(
      `<a href="${noteShowHref(42)}" class="donut-wiki-link" data-wiki-title="/Moon.md" data-wiki-display="Moon" data-note-id="42">Moon</a>`
    )
  })

  it("renders unresolved path Markdown in a scalar as a dead wiki-style link", () => {
    const html = propertyValuePlainToDisplayHtml("[Moon](/Moon.md)", [])
    expect(html).toBe(
      '<a href="#" class="dead-wiki-link" data-wiki-title="/Moon.md" data-wiki-display="Moon">Moon</a>'
    )
  })

  it("round-trips path Markdown from a field root without converting to wiki", () => {
    const root = document.createElement("div")
    root.innerHTML = propertyValuePlainToDisplayHtml("[Moon](/Moon.md)", [
      wikiTitleFromAuthoredToken("[Moon](/Moon.md)", 42),
    ])
    expect(root.querySelector("a.donut-wiki-link")).not.toBeNull()
    expect(serializePropertyValueFieldRoot(root)).toBe("[Moon](/Moon.md)")
  })

  it("does not treat a bare YAML path as a link", () => {
    const html = propertyValuePlainToDisplayHtml("/folder/File.md", [])
    expect(html).not.toContain("donut-wiki-link")
    expect(html).not.toContain("dead-wiki-link")
  })

  it("does not treat malformed nested brackets as a wiki link", () => {
    const plain = "x[[a[b]]y"
    const html = propertyValuePlainToDisplayHtml(plain, [])
    expect(html).not.toContain("donut-wiki-link")
    expect(html).not.toContain("dead-wiki-link")
    expect(html).toContain(escapeHtmlForWikiLinkDisplay(plain))
  })

  it("renders a token only in current text as pending when last-saved markdown is provided", () => {
    const html = propertyValuePlainToDisplayHtml(
      "[[WikiLinks E2E Nowhere]]",
      [],
      "topic: old"
    )
    expect(html).toContain('class="pending-wiki-link"')
    expect(html).not.toContain("dead-wiki-link")
  })

  it("renders unresolved path Markdown as pending when it is only in current text", () => {
    const html = propertyValuePlainToDisplayHtml(
      "[Moon](/Moon.md)",
      [],
      "topic: old"
    )
    expect(html).toContain('class="pending-wiki-link"')
    expect(html).not.toContain("dead-wiki-link")
  })

  it("renders an unresolved token already in last-saved markdown as dead", () => {
    const html = propertyValuePlainToDisplayHtml(
      "[[WikiLinks E2E Nowhere]]",
      [],
      'topic: "[[WikiLinks E2E Nowhere]]"'
    )
    expect(html).toContain('class="dead-wiki-link"')
    expect(html).not.toContain("pending-wiki-link")
  })

  it("keeps a wikiTitles hit live even when last-saved markdown is provided", () => {
    const html = propertyValuePlainToDisplayHtml(
      "[[My Note]]",
      [wikiTitleFromAuthoredToken("My Note", 42)],
      "topic: old"
    )
    expect(html).toContain("donut-wiki-link")
    expect(html).not.toContain("pending-wiki-link")
    expect(html).not.toContain("dead-wiki-link")
  })

  it("round-trips pending wiki anchors from a field root", () => {
    const root = document.createElement("div")
    root.innerHTML = propertyValuePlainToDisplayHtml("[[Ghost]]", [], "")
    expect(root.querySelector("a.pending-wiki-link")).not.toBeNull()
    expect(serializePropertyValueFieldRoot(root)).toBe("[[Ghost]]")
  })

  it("resolves wiki markers when title is known", () => {
    const html = propertyValuePlainToDisplayHtml("[[My Note]]", [
      wikiTitleFromAuthoredToken("My Note", 42),
    ])
    expect(html).toContain("donut-wiki-link")
    expect(html).toContain(noteShowHref(42))
    expect(html).toContain('data-note-id="42"')
    expect(html).toContain('class="wiki-bracket"')
  })

  it("resolves piped wiki marker using target and shows display as visible link", () => {
    const html = propertyValuePlainToDisplayHtml("[[Target Page|friendly]]", [
      wikiTitleFromAuthoredToken("Target Page|friendly", 99),
    ])
    expect(html).toContain("donut-wiki-link")
    expect(html).toContain(noteShowHref(99))
    expect(html).toContain("friendly")
    expect(html).not.toContain("Target Page|friendly")
  })

  it("round-trips mixed text and wiki anchors from a field root", () => {
    const root = document.createElement("div")
    root.innerHTML = propertyValuePlainToDisplayHtml("A [[B]] C", [])
    expect(serializePropertyValueFieldRoot(root)).toBe("A [[B]] C")
  })

  it("serializes live link anchors from visible text (textContent)", () => {
    const root = document.createElement("div")
    root.innerHTML = propertyValuePlainToDisplayHtml("[[N]]", [
      wikiTitleFromAuthoredToken("N", 1),
    ])
    expect(serializePropertyValueFieldRoot(root)).toBe("[[N]]")
  })

  it("serializes a wiki anchor as plain text when the user replaced inner content (broken link)", () => {
    const root = document.createElement("div")
    root.innerHTML = propertyValuePlainToDisplayHtml("[[English]]", [
      wikiTitleFromAuthoredToken("English", 1),
    ])
    const a = root.querySelector("a.donut-wiki-link") as HTMLAnchorElement
    a.textContent = "[[Eng]"
    expect(serializePropertyValueFieldRoot(root)).toBe("[[Eng]")
  })

  it("deadWikiLinkPayloadFromAnchor uses visible closed wiki text", () => {
    const wrap = document.createElement("div")
    wrap.innerHTML = propertyValuePlainToDisplayHtml("see [[X]]", [])
    const a = wrap.querySelector("a.dead-wiki-link") as HTMLAnchorElement
    expect(deadWikiLinkPayloadFromAnchor(a).targetToken).toBe("X")
  })
})

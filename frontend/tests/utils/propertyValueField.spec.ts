import { describe, expect, it } from "vitest"
import {
  propertyValuePlainToDisplayHtml,
  serializePropertyValueFieldRoot,
} from "@/utils/propertyValueField"
import {
  deadWikiLinkPayloadFromAnchor,
  escapeHtmlForWikiLinkDisplay,
  wikiTitleFromInnerAndNoteId,
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

  it("leaves path Markdown in a scalar as escaped text", () => {
    const html = propertyValuePlainToDisplayHtml("[Moon](/Moon.md)", [])
    expect(html).not.toContain("doughnut-wiki-link")
    expect(html).not.toContain("dead-wiki-link")
    expect(html).toBe(escapeHtmlForWikiLinkDisplay("[Moon](/Moon.md)"))
  })

  it("does not treat malformed nested brackets as a wiki link", () => {
    const plain = "x[[a[b]]y"
    const html = propertyValuePlainToDisplayHtml(plain, [])
    expect(html).not.toContain("doughnut-wiki-link")
    expect(html).not.toContain("dead-wiki-link")
    expect(html).toContain(escapeHtmlForWikiLinkDisplay(plain))
  })

  it("resolves wiki markers when title is known", () => {
    const html = propertyValuePlainToDisplayHtml("[[My Note]]", [
      wikiTitleFromInnerAndNoteId("My Note", 42),
    ])
    expect(html).toContain("doughnut-wiki-link")
    expect(html).toContain("/n42")
    expect(html).toContain('class="wiki-bracket"')
  })

  it("resolves piped wiki marker using target and shows display as visible link", () => {
    const html = propertyValuePlainToDisplayHtml("[[Target Page|friendly]]", [
      wikiTitleFromInnerAndNoteId("Target Page|friendly", 99),
    ])
    expect(html).toContain("doughnut-wiki-link")
    expect(html).toContain("/n99")
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
      wikiTitleFromInnerAndNoteId("N", 1),
    ])
    expect(serializePropertyValueFieldRoot(root)).toBe("[[N]]")
  })

  it("serializes a wiki anchor as plain text when the user replaced inner content (broken link)", () => {
    const root = document.createElement("div")
    root.innerHTML = propertyValuePlainToDisplayHtml("[[English]]", [
      wikiTitleFromInnerAndNoteId("English", 1),
    ])
    const a = root.querySelector("a.doughnut-wiki-link") as HTMLAnchorElement
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

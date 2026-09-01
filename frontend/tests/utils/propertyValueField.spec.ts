import { describe, expect, it } from "vitest"
import { noteShowHref } from "@/routes/noteShowLocation"
import {
  propertyValuePlainToDisplayHtml,
  serializePropertyValueFieldRoot,
} from "@/utils/propertyValueField"
import {
  deadWikiLinkPayloadFromAnchor,
  escapeHtmlForWikiLinkDisplay,
  wikiLinkAnchorHtml,
  wikiLinkBracketedInnerHtml,
  wikiLinkFromAuthoredToken,
} from "@/utils/wikiLinkMarkup"

describe("propertyValueField utils", () => {
  it("renders unresolved wiki link with display text after pipe", () => {
    const html = propertyValuePlainToDisplayHtml(
      "See [[Unknown Topic|friendly label]] here",
      []
    )
    expect(html).toContain('class="dead-wiki-link"')
    expect(html).toContain('data-portable-path="Unknown Topic"')
    expect(html).toContain('data-display-text="friendly label"')
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

  it("deadWikiLinkPayloadFromAnchor prefers data-portable-path for piped links", () => {
    const wrap = document.createElement("div")
    wrap.innerHTML = propertyValuePlainToDisplayHtml("[[Missing|Shown]]", [])
    const a = wrap.querySelector("a.dead-wiki-link") as HTMLAnchorElement
    expect(deadWikiLinkPayloadFromAnchor(a).portablePath).toBe("Missing")
  })

  it("turns only well-formed wiki markers into dead-wiki-link anchors with visible brackets", () => {
    const html = propertyValuePlainToDisplayHtml("See [[X]] here", [])
    const innerHtml = wikiLinkBracketedInnerHtml("X")
    expect(html).toContain(innerHtml)
    expect(html).toBe(
      `See ${wikiLinkAnchorHtml({
        href: "#",
        className: "dead-wiki-link",
        portablePath: "X",
        display: "X",
        innerHtml,
      })} here`
    )
  })

  it("does not treat empty or whitespace-only brackets as a wiki link", () => {
    const html = propertyValuePlainToDisplayHtml("A [[ ]] B [[  ]]", [])
    expect(html).not.toContain("dead-wiki-link")
    expect(html).toContain("[[ ]]")
  })

  it("leaves path-shaped Markdown as plain text in a scalar", () => {
    const html = propertyValuePlainToDisplayHtml("[Moon](/Moon.md)", [
      {
        authoredLink: "[Moon](/Moon.md)",
        portablePath: "/Moon.md",
        displayText: "Moon",
        resolution: "RESOLVED",
        destinationNoteId: 42,
      },
    ])
    expect(html).toBe(escapeHtmlForWikiLinkDisplay("[Moon](/Moon.md)"))
    expect(html).not.toContain("donut-wiki-link")
    expect(html).not.toContain("dead-wiki-link")
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

  it("renders an unresolved token already in last-saved markdown as dead", () => {
    const html = propertyValuePlainToDisplayHtml(
      "[[WikiLinks E2E Nowhere]]",
      [],
      'topic: "[[WikiLinks E2E Nowhere]]"'
    )
    expect(html).toContain('class="dead-wiki-link"')
    expect(html).not.toContain("pending-wiki-link")
  })

  it("keeps a wikiLinks hit live even when last-saved markdown is provided", () => {
    const html = propertyValuePlainToDisplayHtml(
      "[[My Note]]",
      [wikiLinkFromAuthoredToken("My Note", 42)],
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
      wikiLinkFromAuthoredToken("My Note", 42),
    ])
    expect(html).toContain("donut-wiki-link")
    expect(html).toContain(noteShowHref(42))
    expect(html).toContain('data-note-id="42"')
  })

  it("resolves piped wiki marker using target and shows display as visible link", () => {
    const html = propertyValuePlainToDisplayHtml("[[Target Page|friendly]]", [
      wikiLinkFromAuthoredToken("Target Page|friendly", 99),
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
      wikiLinkFromAuthoredToken("N", 1),
    ])
    expect(serializePropertyValueFieldRoot(root)).toBe("[[N]]")
  })

  it("serializes a wiki anchor as plain text when the user replaced inner content (broken link)", () => {
    const root = document.createElement("div")
    root.innerHTML = propertyValuePlainToDisplayHtml("[[English]]", [
      wikiLinkFromAuthoredToken("English", 1),
    ])
    const a = root.querySelector("a.donut-wiki-link") as HTMLAnchorElement
    a.textContent = "[[Eng]"
    expect(serializePropertyValueFieldRoot(root)).toBe("[[Eng]")
  })

  it("deadWikiLinkPayloadFromAnchor uses visible closed wiki text", () => {
    const wrap = document.createElement("div")
    wrap.innerHTML = propertyValuePlainToDisplayHtml("see [[X]]", [])
    const a = wrap.querySelector("a.dead-wiki-link") as HTMLAnchorElement
    expect(deadWikiLinkPayloadFromAnchor(a).portablePath).toBe("X")
  })
})

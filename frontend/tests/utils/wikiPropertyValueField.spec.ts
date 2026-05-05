import { describe, expect, it } from "vitest"
import {
  deadLinkCreateTitleFromAnchor,
  escapeHtmlForWikiPropertyValue,
  propertyValuePlainToDisplayHtml,
  serializeWikiPropertyValueFieldRoot,
} from "@/utils/wikiPropertyValueField"

describe("wikiPropertyValueField utils", () => {
  it("escapes HTML-sensitive characters for display pipeline", () => {
    expect(escapeHtmlForWikiPropertyValue(`a<b>"c`)).toBe("a&lt;b&gt;&quot;c")
  })

  it("turns only well-formed wiki markers into dead-link anchors with visible brackets", () => {
    const html = propertyValuePlainToDisplayHtml("See [[X]] here", [])
    expect(html).toContain('class="dead-link"')
    expect(html).toContain('class="wiki-bracket"')
    expect(html).toContain("data-wiki-title")
    expect(html).toContain("X")
    expect(html).toContain("See ")
  })

  it("does not treat empty or whitespace-only brackets as a wiki link", () => {
    const html = propertyValuePlainToDisplayHtml("A [[ ]] B [[  ]]", [])
    expect(html).not.toContain("dead-link")
    expect(html).toContain("[[ ]]")
  })

  it("does not treat malformed nested brackets as a wiki link", () => {
    const plain = "x[[a[b]]y"
    const html = propertyValuePlainToDisplayHtml(plain, [])
    expect(html).not.toContain("doughnut-link")
    expect(html).not.toContain("dead-link")
    expect(html).toContain(escapeHtmlForWikiPropertyValue(plain))
  })

  it("resolves wiki markers when title is known", () => {
    const html = propertyValuePlainToDisplayHtml("[[My Note]]", [
      { linkText: "My Note", noteId: 42 },
    ])
    expect(html).toContain("doughnut-link")
    expect(html).toContain("/d/n/42")
    expect(html).toContain('class="wiki-bracket"')
  })

  it("round-trips mixed text and wiki anchors from a field root", () => {
    const root = document.createElement("div")
    root.innerHTML = propertyValuePlainToDisplayHtml("A [[B]] C", [])
    expect(serializeWikiPropertyValueFieldRoot(root)).toBe("A [[B]] C")
  })

  it("serializes live link anchors from visible text (textContent)", () => {
    const root = document.createElement("div")
    root.innerHTML = propertyValuePlainToDisplayHtml("[[N]]", [
      { linkText: "N", noteId: 1 },
    ])
    expect(serializeWikiPropertyValueFieldRoot(root)).toBe("[[N]]")
  })

  it("serializes a wiki anchor as plain text when the user replaced inner content (broken link)", () => {
    const root = document.createElement("div")
    root.innerHTML = propertyValuePlainToDisplayHtml("[[English]]", [
      { linkText: "English", noteId: 1 },
    ])
    const a = root.querySelector("a.doughnut-link") as HTMLAnchorElement
    a.textContent = "[[Eng]"
    expect(serializeWikiPropertyValueFieldRoot(root)).toBe("[[Eng]")
  })

  it("deadLinkCreateTitleFromAnchor uses visible closed wiki text", () => {
    const wrap = document.createElement("div")
    wrap.innerHTML = propertyValuePlainToDisplayHtml("see [[X]]", [])
    const a = wrap.querySelector("a.dead-link") as HTMLAnchorElement
    expect(deadLinkCreateTitleFromAnchor(a)).toBe("X")
  })

  it("deadLinkCreateTitleFromAnchor reads title from incomplete visible wiki text", () => {
    const a = document.createElement("a")
    a.className = "dead-link"
    a.textContent = "[[Eng"
    expect(deadLinkCreateTitleFromAnchor(a)).toBe("Eng")
  })
})

import { describe, it, expect } from "vitest"
import htmlToMarkdown from "@/components/form/quillHtmlToMarkdown"
import { replaceWikiLinksInHtml } from "@/components/form/replaceWikiLinksInHtml"

describe("quillHtmlToMarkdown", () => {
  it("preserves escaped HTML entities in markdown output", () => {
    const html = '<p>emit <span class="s1">&lt;br&gt;</span>.</p>'
    const result = htmlToMarkdown(html)
    // The escaped entity should remain escaped in markdown
    expect(result).toContain("\\<br\\>")
  })

  it("preserves escaped HTML entities inside span with nested tags", () => {
    const html = '<p><span class="s1"><b>&lt;br&gt;</b></span></p>'
    const result = htmlToMarkdown(html)
    // The escaped entity should remain escaped even when span contains nested HTML tags
    expect(result).toContain("\\<br\\>")
  })

  it("preserves escaped HTML entities inside span with h3 nested tag", () => {
    const html =
      '<p><span class="s1"><h3><b>text with &lt;br&gt; here</b></h3></span></p>'
    const result = htmlToMarkdown(html)
    // The escaped entity should remain escaped even when span contains h3 and b tags
    expect(result).toContain("\\<br\\>")
  })

  it("converts HTML with code blocks separated by hr to separate markdown code blocks", () => {
    const html =
      '<div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain">A</div></div><p><hr></p><div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain">B</div></div>'
    const result = htmlToMarkdown(html)
    // Should contain a code block with A only
    expect(result).toContain("```\nA\n```")
    // Should contain a code block with B only
    expect(result).toContain("```\nB\n```")
    // Should not merge A and B into a single code block
    expect(result).not.toContain("```\nA\nB\n```")
  })

  /** Punctuation / Turndown escaping: given Quill HTML → then markdown. */
  const whenHtmlIsConvertedToMarkdown = "htmlToMarkdown"

  it.each`
    label             | given                | when                             | then
    ${"< via entity"} | ${"<p>&lt;test</p>"} | ${whenHtmlIsConvertedToMarkdown} | ${"<test"}
    ${"> via entity"} | ${"<p>test&gt;</p>"} | ${whenHtmlIsConvertedToMarkdown} | ${"test>"}
    ${"["}            | ${"<p>[test</p>"}    | ${whenHtmlIsConvertedToMarkdown} | ${"\\[test"}
    ${"]"}            | ${"<p>test]</p>"}    | ${whenHtmlIsConvertedToMarkdown} | ${"test\\]"}
    ${"[[ with text"} | ${"<p>[[test</p>"}   | ${whenHtmlIsConvertedToMarkdown} | ${"\\[\\[test"}
    ${"]] with text"} | ${"<p>test]]</p>"}   | ${whenHtmlIsConvertedToMarkdown} | ${"test\\]\\]"}
  `("$label (when: $when)", ({ given, then }) => {
    expect(htmlToMarkdown(given)).toBe(then)
  })

  it("preserves complete double brackets as WikiLink syntax", () => {
    const html = "<p>[[WikiLink]]</p>"
    const result = htmlToMarkdown(html)
    expect(result).toBe("[[WikiLink]]")
  })

  it("converts internal note wiki anchors to wikilink markdown", () => {
    expect(htmlToMarkdown('<p><a href="/n123">MyNote</a></p>')).toBe(
      "[[MyNote]]"
    )
  })

  it("converts dead wiki anchors to wikilink markdown", () => {
    expect(
      htmlToMarkdown('<p><a href="#" class="dead-link">Unknown</a></p>')
    ).toBe("[[Unknown]]")
  })

  it("linkified wikilinks: two wikilinks in one paragraph", () => {
    const raw = "<p>[[LeSS in Action]] .... [[Odd-e CSD]]</p>"
    const html = replaceWikiLinksInHtml(raw, [
      { title: "LeSS in Action", noteId: 1 },
      { title: "Odd-e CSD", noteId: 2 },
    ])
    expect(htmlToMarkdown(html)).toBe("[[LeSS in Action]] .... [[Odd-e CSD]]")
  })

  it("linkified wikilinks: extra [ before a resolved wikilink", () => {
    const raw = "<p>[[[WikiLink]]</p>"
    const html = replaceWikiLinksInHtml(raw, [
      { title: "WikiLink", noteId: 99 },
    ])
    expect(htmlToMarkdown(html)).toBe(String.raw`\[[[WikiLink]]`)
  })

  it("linkified wikilinks: extra ] after a resolved wikilink", () => {
    const raw = "<p>[[WikiLink]]]</p>"
    const html = replaceWikiLinksInHtml(raw, [
      { title: "WikiLink", noteId: 99 },
    ])
    expect(htmlToMarkdown(html)).toBe("[[WikiLink]]\\]")
  })

  it("linkified wikilinks: extra [ before and ] after a resolved wikilink", () => {
    const raw = "<p>[[[WikiLink]]]</p>"
    const html = replaceWikiLinksInHtml(raw, [
      { title: "WikiLink", noteId: 99 },
    ])
    expect(htmlToMarkdown(html)).toBe(String.raw`\[[[WikiLink]]\]`)
  })
})

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

  it.each`
    label                                   | html                                                                          | expected
    ${"preserves complete double brackets"} | ${"<p>[[WikiLink]]</p>"}                                                      | ${"[[WikiLink]]"}
    ${"converts doughnut-link anchors"}     | ${'<p><a href="/d/notebooks/7/notes/x" class="doughnut-link">MyNote</a></p>'} | ${"[[MyNote]]"}
    ${"note href without doughnut-link"}    | ${'<p><a href="/n123">looks internal</a></p>'}                                | ${"[looks internal](/n123)"}
    ${"converts dead wiki anchors"}         | ${'<p><a href="#" class="dead-link">Unknown</a></p>'}                         | ${"[[Unknown]]"}
  `("wiki links: $label", ({ html, expected }) => {
    expect(htmlToMarkdown(html)).toBe(expected)
  })

  const linkifiedTwoNotes = [
    { linkText: "LeSS in Action", notebookId: 1, slug: "less" },
    { linkText: "Odd-e CSD", notebookId: 2, slug: "csd" },
  ] as const
  const linkifiedWikiLink99 = [
    { linkText: "WikiLink", notebookId: 99, slug: "wikilink" },
  ] as const

  it.each`
    label                               | raw                                               | resolves               | expected
    ${"two wikilinks in one paragraph"} | ${"<p>[[LeSS in Action]] .... [[Odd-e CSD]]</p>"} | ${linkifiedTwoNotes}   | ${"[[LeSS in Action]] .... [[Odd-e CSD]]"}
    ${"extra [ before resolved"}        | ${"<p>[[[WikiLink]]</p>"}                         | ${linkifiedWikiLink99} | ${String.raw`\[[[WikiLink]]`}
    ${"extra ] after resolved"}         | ${"<p>[[WikiLink]]]</p>"}                         | ${linkifiedWikiLink99} | ${"[[WikiLink]]\\]"}
    ${"extra [ before and ] after"}     | ${"<p>[[[WikiLink]]]</p>"}                        | ${linkifiedWikiLink99} | ${String.raw`\[[[WikiLink]]\]`}
  `("linkified wiki links: $label", ({ raw, resolves, expected }) => {
    const html = replaceWikiLinksInHtml(raw, [...resolves])
    expect(htmlToMarkdown(html)).toBe(expected)
  })
})

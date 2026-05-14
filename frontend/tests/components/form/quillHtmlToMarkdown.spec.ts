import { describe, it, expect } from "vitest"
import htmlToMarkdown from "@/components/form/quillHtmlToMarkdown"
import { replaceWikiLinksInHtml } from "@/components/form/replaceWikiLinksInHtml"
import { wikiTitleFromInnerAndNoteId } from "@/utils/wikiPropertyValueField"

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
    label                                     | html                                                                                                                                                    | expected
    ${"preserves complete double brackets"}   | ${"<p>[[WikiLink]]</p>"}                                                                                                                                | ${"[[WikiLink]]"}
    ${"converts doughnut-link anchors"}       | ${'<p><a href="/d/n/701" class="doughnut-link">MyNote</a></p>'}                                                                                         | ${"[[MyNote]]"}
    ${"note /d/n href without doughnut-link"} | ${'<p><a href="/d/n/701">MyNote</a></p>'}                                                                                                               | ${"[[MyNote]]"}
    ${"absolute URL to note show"}            | ${'<p><a href="https://app.test/d/n/42">T</a></p>'}                                                                                                     | ${"[[T]]"}
    ${"note href without doughnut-link"}      | ${'<p><a href="/n123">looks internal</a></p>'}                                                                                                          | ${"[looks internal](/n123)"}
    ${"converts dead wiki anchors"}           | ${'<p><a href="#" class="dead-link" data-wiki-title="Unknown"><span class="wiki-bracket">[[</span>Unknown<span class="wiki-bracket">]]</span></a></p>'} | ${"[[Unknown]]"}
    ${"converts plain dead wiki anchors"}     | ${'<p><a href="#" class="dead-link" data-wiki-title="Unknown">Unknown</a></p>'}                                                                         | ${"[[Unknown]]"}
    ${"doughnut-link with piped wiki attrs"}  | ${'<p><a href="/d/n/1" class="doughnut-link" data-wiki-title="A" data-wiki-display="B">B</a></p>'}                                                      | ${"[[A|B]]"}
  `("wiki links: $label", ({ html, expected }) => {
    expect(htmlToMarkdown(html)).toBe(expected)
  })

  const linkifiedTwoNotes = [
    wikiTitleFromInnerAndNoteId("LeSS in Action", 101),
    wikiTitleFromInnerAndNoteId("Odd-e CSD", 202),
  ]
  const linkifiedWikiLink99 = [wikiTitleFromInnerAndNoteId("WikiLink", 9901)]
  const linkifiedPipedResolved = [
    wikiTitleFromInnerAndNoteId("MyTarget|shown text", 44),
  ]

  it.each`
    label                               | raw                                               | resolves                  | expected
    ${"two wikilinks in one paragraph"} | ${"<p>[[LeSS in Action]] .... [[Odd-e CSD]]</p>"} | ${linkifiedTwoNotes}      | ${"[[LeSS in Action]] .... [[Odd-e CSD]]"}
    ${"extra [ before resolved"}        | ${"<p>[[[WikiLink]]</p>"}                         | ${linkifiedWikiLink99}    | ${String.raw`\[[[WikiLink]]`}
    ${"extra ] after resolved"}         | ${"<p>[[WikiLink]]]</p>"}                         | ${linkifiedWikiLink99}    | ${"[[WikiLink]]\\]"}
    ${"extra [ before and ] after"}     | ${"<p>[[[WikiLink]]]</p>"}                        | ${linkifiedWikiLink99}    | ${String.raw`\[[[WikiLink]]\]`}
    ${"piped resolved round-trip"}      | ${"<p>[[MyTarget|shown text]]</p>"}               | ${linkifiedPipedResolved} | ${"[[MyTarget|shown text]]"}
    ${"piped unresolved stays piped"}   | ${"<p>[[Unknown Topic|friendly label]]</p>"}      | ${[]}                     | ${"[[Unknown Topic|friendly label]]"}
  `("linkified wiki links: $label", ({ raw, resolves, expected }) => {
    const html = replaceWikiLinksInHtml(raw, [...resolves])
    expect(htmlToMarkdown(html)).toBe(expected)
  })
})

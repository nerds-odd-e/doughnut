import { describe, it, expect } from "vitest"
import htmlToMarkdown from "@/components/form/quillHtmlToMarkdown"
import { replaceWikiLinksInHtml } from "@/components/form/replaceWikiLinksInHtml"
import { noteShowHref } from "@/routes/noteShowLocation"
import { wikiTitleFromAuthoredToken } from "@/utils/wikiLinkMarkup"

describe("quillHtmlToMarkdown", () => {
  it("preserves escaped HTML entities in markdown output", () => {
    expect(
      htmlToMarkdown('<p>emit <span class="s1">&lt;br&gt;</span>.</p>')
    ).toContain("\\<br\\>")
  })

  it("preserves escaped HTML entities inside span with nested tags", () => {
    expect(
      htmlToMarkdown('<p><span class="s1"><b>&lt;br&gt;</b></span></p>')
    ).toContain("\\<br\\>")
  })

  it("preserves escaped HTML entities inside span with h3 nested tag", () => {
    expect(
      htmlToMarkdown(
        '<p><span class="s1"><h3><b>text with &lt;br&gt; here</b></h3></span></p>'
      )
    ).toContain("\\<br\\>")
  })

  it("converts HTML with code blocks separated by hr to separate markdown code blocks", () => {
    const result = htmlToMarkdown(
      '<div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain">A</div></div><p><hr></p><div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain">B</div></div>'
    )
    expect(result).toContain("```\nA\n```")
    expect(result).toContain("```\nB\n```")
    expect(result).not.toContain("```\nA\nB\n```")
  })

  it.each`
    label             | given                | then
    ${"< via entity"} | ${"<p>&lt;test</p>"} | ${"<test"}
    ${"> via entity"} | ${"<p>test&gt;</p>"} | ${"test>"}
    ${"["}            | ${"<p>[test</p>"}    | ${"\\[test"}
    ${"]"}            | ${"<p>test]</p>"}    | ${"test\\]"}
    ${"[[ with text"} | ${"<p>[[test</p>"}   | ${"\\[\\[test"}
    ${"]] with text"} | ${"<p>test]]</p>"}   | ${"test\\]\\]"}
  `("$label punctuation escaping", ({ given, then }) => {
    expect(htmlToMarkdown(given)).toBe(then)
  })

  it.each`
    label                                             | html                                                                                                                                                         | expected
    ${"preserves complete double brackets"}           | ${"<p>[[WikiLink]]</p>"}                                                                                                                                     | ${"[[WikiLink]]"}
    ${"converts donut-wiki-link anchors"}             | ${'<p><a href="/n701" class="donut-wiki-link">MyNote</a></p>'}                                                                                               | ${"[[MyNote]]"}
    ${"note show href without donut-wiki-link"}       | ${'<p><a href="/n701">MyNote</a></p>'}                                                                                                                       | ${"[[MyNote]]"}
    ${"absolute URL to note show"}                    | ${'<p><a href="https://app.test/n42">T</a></p>'}                                                                                                             | ${"[[T]]"}
    ${"note href without donut-wiki-link"}            | ${'<p><a href="/n123">looks internal</a></p>'}                                                                                                               | ${"[[looks internal]]"}
    ${"converts dead wiki anchors"}                   | ${'<p><a href="#" class="dead-wiki-link" data-wiki-title="Unknown"><span class="wiki-bracket">[[</span>Unknown<span class="wiki-bracket">]]</span></a></p>'} | ${"[[Unknown]]"}
    ${"converts plain dead wiki anchors"}             | ${'<p><a href="#" class="dead-wiki-link" data-wiki-title="Unknown">Unknown</a></p>'}                                                                         | ${"[[Unknown]]"}
    ${"converts pending wiki anchors"}                | ${'<p><a href="#" class="pending-wiki-link" data-wiki-title="Unknown">Unknown</a></p>'}                                                                      | ${"[[Unknown]]"}
    ${"donut-wiki-link with piped wiki attrs"}        | ${'<p><a href="/n1" class="donut-wiki-link" data-wiki-title="A" data-wiki-display="B">B</a></p>'}                                                            | ${"[[A|B]]"}
    ${"path markdown donut-wiki-link keeps markdown"} | ${'<p><a href="/Folder/Title.md" class="donut-wiki-link" data-wiki-title="/Folder/Title.md" data-wiki-display="label" data-note-id="42">label</a></p>'}      | ${"[label](/Folder/Title.md)"}
    ${"live path markdown with noteShowHref"}         | ${`<p><a href="${noteShowHref(42)}" class="donut-wiki-link" data-wiki-title="/Folder/Title.md" data-wiki-display="label" data-note-id="42">label</a></p>`}   | ${"[label](/Folder/Title.md)"}
    ${"path markdown without .md keeps href"}         | ${'<p><a href="/Folder/Title" class="donut-wiki-link" data-wiki-title="/Folder/Title" data-wiki-display="label">label</a></p>'}                              | ${"[label](/Folder/Title)"}
    ${"path markdown dead-wiki-link keeps markdown"}  | ${'<p><a href="/Folder/Missing.md" class="dead-wiki-link" data-wiki-title="/Folder/Missing.md" data-wiki-display="label">label</a></p>'}                     | ${"[label](/Folder/Missing.md)"}
    ${"path markdown dead with hash href"}            | ${'<p><a href="#" class="dead-wiki-link" data-wiki-title="/Folder/Missing.md" data-wiki-display="label">label</a></p>'}                                      | ${"[label](/Folder/Missing.md)"}
  `("wiki links: $label", ({ html, expected }) => {
    expect(htmlToMarkdown(html)).toBe(expected)
  })

  const linkifiedTwoNotes = [
    wikiTitleFromAuthoredToken("LeSS in Action", 101),
    wikiTitleFromAuthoredToken("Odd-e CSD", 202),
  ]
  const linkifiedWikiLink99 = [wikiTitleFromAuthoredToken("WikiLink", 9901)]
  const linkifiedPipedResolved = [
    wikiTitleFromAuthoredToken("MyTarget|shown text", 44),
  ]

  it.each`
    label                               | raw                                                | resolves                  | expected
    ${"two wikilinks in one paragraph"} | ${"<p>[[LeSS in Action]] .... [[Odd-e CSD]]</p>"}  | ${linkifiedTwoNotes}      | ${"[[LeSS in Action]] .... [[Odd-e CSD]]"}
    ${"extra [ before resolved"}        | ${"<p>[[[WikiLink]]</p>"}                          | ${linkifiedWikiLink99}    | ${String.raw`\[[[WikiLink]]`}
    ${"extra ] after resolved"}         | ${"<p>[[WikiLink]]]</p>"}                          | ${linkifiedWikiLink99}    | ${"[[WikiLink]]\\]"}
    ${"extra [ before and ] after"}     | ${"<p>[[[WikiLink]]]</p>"}                         | ${linkifiedWikiLink99}    | ${String.raw`\[[[WikiLink]]\]`}
    ${"piped resolved round-trip"}      | ${"<p>[[MyTarget|shown text]]</p>"}                | ${linkifiedPipedResolved} | ${"[[MyTarget|shown text]]"}
    ${"piped unresolved stays piped"}   | ${"<p>[[Unknown Topic|friendly label]]</p>"}       | ${[]}                     | ${"[[Unknown Topic|friendly label]]"}
    ${"unresolved path markdown stays"} | ${'<p><a href="/Folder/Missing.md">label</a></p>'} | ${[]}                     | ${"[label](/Folder/Missing.md)"}
  `("linkified wiki links: $label", ({ raw, resolves, expected }) => {
    const html = replaceWikiLinksInHtml(raw, [...resolves])
    expect(htmlToMarkdown(html)).toBe(expected)
  })

  it("round-trips a pending wiki link to authored wiki markup", () => {
    const html = replaceWikiLinksInHtml(
      "<p>[[WikiLinks E2E Nowhere]]</p>",
      [],
      ""
    )
    expect(html).toContain("pending-wiki-link")
    expect(htmlToMarkdown(html)).toBe("[[WikiLinks E2E Nowhere]]")
  })
})

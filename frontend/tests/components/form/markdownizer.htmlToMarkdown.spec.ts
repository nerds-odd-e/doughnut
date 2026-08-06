import markdownizer from "@/components/form/markdownizer"
import { describe, it, expect } from "vitest"

describe("markdownizer htmlToMarkdown", () => {
  it("converts basic HTML to markdown", () => {
    expect(
      markdownizer.htmlToMarkdown(
        "<h1>Hello World</h1><p>This is <em>markdown</em>.</p>"
      )
    ).toBe("Hello World\n===========\n\nThis is _markdown_.")
  })

  it("converts empty lines with br", () => {
    expect(markdownizer.htmlToMarkdown("<p>a</p><p><br></p><p>b</p>")).toBe(
      "a\n\n<br>\n\nb"
    )
  })

  it("converts Quill bullet list to markdown", () => {
    expect(
      markdownizer.htmlToMarkdown(
        "<ol><li data-list='bullet'>item1</li><li data-list='bullet'>item2</li></ol>"
      )
    ).toBe("* item1\n* item2")
  })

  it("converts nested Quill list to markdown", () => {
    expect(
      markdownizer.htmlToMarkdown(
        "<ol><li data-list='bullet'>item1</li><li data-list='bullet' class='ql-indent-1'>item1.1</li></ol>"
      )
    ).toBe("* item1\n  * item1.1")
  })

  it("converts nested h1 tags to single header", () => {
    const markdown = markdownizer.htmlToMarkdown(
      '<p class="p1"><span class="s1"><h1><b>✅<span class="Apple-converted-space"> </span></b></h1><h1><b>Conclusion</b></h1></span></p>'
    )
    expect(markdown.match(/={3,}$/gm)?.length).toBe(1)
  })

  it("keeps separate h1 tags as separate headers", () => {
    const markdown = markdownizer.htmlToMarkdown(
      "<h1>Chapter 1</h1><h1>Chapter 2</h1>"
    )
    expect(markdown.match(/={3,}$/gm)?.length).toBe(2)
  })

  describe("code block conversions", () => {
    it("converts HTML code block with blank line to markdown", () => {
      const markdown = markdownizer.htmlToMarkdown(
        "<pre><code>hello\n\nwork\n</code></pre>"
      )
      expect(markdown).toMatch(/```[\s\S]*hello\n\nwork[\s\S]*```/)
    })

    it.each([
      ["<pre> tag", "<pre>content</pre>", "content"],
      [
        "Quill code block",
        '<div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain">Content</div></div>',
        "Content",
      ],
      [
        "Quill code block with leading spaces",
        '<div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain">  indented line</div></div>',
        "  indented line",
      ],
    ])(
      "converts %s to markdown fenced code block",
      (_, html, expectedContent) => {
        const markdown = markdownizer.htmlToMarkdown(html)
        expect(markdown).toContain(expectedContent)
        expect(markdown).toMatch(/```[\s\S]*```/)
      }
    )

    it("does not escape underscore in <pre> tag", () => {
      const markdown = markdownizer.htmlToMarkdown(
        '<pre data-language="plain">\n\n_\n</pre>'
      )
      expect(markdown).toContain("_")
      expect(markdown).not.toContain("\\_")
    })

    it("converts empty Quill code block line to empty markdown line", () => {
      const markdown = markdownizer.htmlToMarkdown(
        '<div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain"><br></div></div>'
      )
      expect(markdown).not.toContain("<br>")
      expect(markdown).toMatch(/```\n\n```/)
    })
  })

  it("converts HTML table to markdown table", () => {
    const markdown = markdownizer.htmlToMarkdown(
      "<table><thead><tr><th>Name</th><th>Score</th></tr></thead><tbody><tr><td>Alice</td><td>95</td></tr><tr><td>Bob</td><td>88</td></tr></tbody></table>"
    )
    expect(markdown).toMatch(/\|.*Name.*\|.*Score.*\|/)
    expect(markdown).toMatch(/\|.*-+.*\|.*-+.*\|/)
    expect(markdown).toMatch(/\|.*Alice.*\|.*95.*\|/)
    expect(markdown).toMatch(/\|.*Bob.*\|.*88.*\|/)
  })

  it("converts HTML table with nested p and b tags to markdown table", () => {
    const html = `<table><thead><tr><th>
<p class="p1"><b>Item</b></p>
</th><th>
<p class="p1"><b>Value</b></p>
</th></tr></thead><tbody><tr><td>
<p class="p1">A</p>
</td><td>
<p class="p1">17</p>
</td></tr></tbody></table>`
    const markdown = markdownizer.htmlToMarkdown(html)
    expect(markdown).toMatch(/\*\*Item\*\*/)
    expect(markdown).toMatch(/\|[\s\S]*A[\s\S]*\|[\s\S]*17[\s\S]*\|/)
  })
})

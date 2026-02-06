import markdownizer from "@/components/form/markdownizer"
import { describe, it, expect } from "vitest"

const toHtml = (markdown: string | undefined) =>
  markdownizer.markdownToHtml(markdown)

const toHtmlElement = (markdown: string) => {
  const div = document.createElement("div")
  div.innerHTML = toHtml(markdown)
  return div
}

describe("Markdown and HTML Conversion Tests", () => {
  describe("round-trip conversion (pasted HTML -> markdown -> HTML)", () => {
    it("wraps standalone br in paragraph when before list", () => {
      const pastedHtml = `<p><br></p><ul><li>Item 1</li><li>Item 2</li></ul>`
      const markdown = markdownizer.htmlToMarkdown(pastedHtml)
      const html = markdownizer.markdownToHtml(markdown, { preserve_pre: true })
      const div = document.createElement("div")
      div.innerHTML = html

      expect(html).toMatch(/<p>.*<br.*>.*<\/p>/)
      expect(html).not.toMatch(/<li[^>]*>\s*<br/)
      expect(div.querySelector("br")?.parentElement?.tagName).toBe("P")
      expect(div.querySelector("li")?.textContent?.trim()).toBe("Item 1")
    })
  })

  describe("markdown to HTML", () => {
    it.each([
      [
        "basic markdown",
        "# Hello World\n\nThis is *markdown*.",
        "<h1>Hello World</h1><p>This is <em>markdown</em>.</p>",
      ],
      ["undefined input", undefined, ""],
      [
        "raw HTML tags (escaped)",
        "raw <span> is ok.",
        "<p>raw &lt;span&gt; is ok.</p>",
      ],
    ])("converts %s", (_, markdown, expected) => {
      expect(toHtml(markdown)).toBe(expected)
    })

    describe("list rendering as Quill editor format", () => {
      it("renders bullet list", () => {
        const ol = toHtmlElement("* item1\n* item2\n").querySelector("ol")
        expect(ol?.querySelectorAll("li").length).toBe(2)
        expect(ol?.querySelector("li")).toHaveAttribute("data-list", "bullet")
      })

      it("renders ordered list item", () => {
        expect(toHtmlElement("2. item1").querySelector("li")).toHaveAttribute(
          "data-list",
          "ordered"
        )
      })

      it("renders nested ordered list item", () => {
        expect(
          toHtmlElement("* level1\n  2. item1").querySelector(
            "li[data-list='ordered']"
          )
        ).not.toBeNull()
      })

      it("renders nested list with ql-indent class", () => {
        const elm = toHtmlElement("* item1\n  * item1.1\n")
        expect(elm.querySelectorAll("ol").length).toBe(1)
        expect(elm.querySelector("li.ql-indent-1")).not.toBeNull()
      })

      it("renders multiple level nested list", () => {
        const elm = toHtmlElement("* item1\n  * item1.1\n    * item1.1.1\n")
        expect(elm.querySelectorAll("ol").length).toBe(1)
        expect(elm.querySelector("li.ql-indent-2")).not.toBeNull()
      })

      it("renders raw HTML ul/li as Quill format", () => {
        const elm = toHtmlElement("<ul><li>list item</li></ul>")
        expect(elm.querySelectorAll("ol").length).toBe(1)
        expect(elm.querySelector("li[data-list='bullet']")).not.toBeNull()
      })
    })

    it("renders markdown table as HTML", () => {
      expect(
        toHtml(
          `| Name    | Score |\n| ------- | ----- |\n| Alice   |  95   |\n| Bob     |  88   |`
        )
      ).toMatchInlineSnapshot(
        `"<table><thead><tr><th>Name</th><th>Score</th></tr></thead><tbody><tr><td>Alice</td><td>95</td></tr><tr><td>Bob</td><td>88</td></tr></tbody></table>"`
      )
    })

    it("renders markdown table with proper DOM structure", () => {
      const elm = toHtmlElement(`| Item | Value |\n| --- | --- |\n| A | 1 |`)
      expect(elm.querySelectorAll("thead th").length).toBe(2)
      expect(elm.querySelector("thead th")?.textContent).toBe("Item")
      expect(elm.querySelectorAll("tbody td").length).toBe(2)
      expect(elm.querySelector("tbody td")?.textContent).toBe("A")
    })

    describe("blockquotes remove <p> tags", () => {
      it.each([
        [
          "simple",
          "> This is a quote",
          "<blockquote>This is a quote</blockquote>",
        ],
        [
          "with formatting",
          "> This is a *quote* with **formatting**",
          "<blockquote>This is a <em>quote</em> with <strong>formatting</strong></blockquote>",
        ],
      ])("%s blockquote", (_, markdown, expected) => {
        expect(toHtml(markdown)).toBe(expected)
      })

      it("multi-line blockquote", () => {
        const html = toHtml("> This is a quote\n> with multiple lines")
        expect(html).not.toContain("<p>")
        expect(html).not.toContain("</p>")
      })
    })

    describe("CJK underscore handling", () => {
      it.each([
        [
          "adjacent to CJK characters",
          "これは_重要_なことです",
          "<p>これは_重要_なことです</p>",
        ],
        ["after CJK opening bracket", "「_水曜日_」", "<p>「_水曜日_」</p>"],
        [
          "after Japanese period",
          "日本語。_日本語_",
          "<p>日本語。_日本語_</p>",
        ],
        ["after Japanese comma", "、_水曜日_", "<p>、_水曜日_</p>"],
        [
          "inside fullwidth parentheses",
          "読むこと（_read_）",
          "<p>読むこと（_read_）</p>",
        ],
        [
          "in mixed CJK sentence",
          "てっきり今日は水曜日だ_とばかり思っていました_。",
          "<p>てっきり今日は水曜日だ_とばかり思っていました_。</p>",
        ],
      ])("does not treat underscores as emphasis %s", (_, markdown, expected) => {
        const html = toHtml(markdown)
        expect(html).toBe(expected)
        expect(html).not.toContain("<em>")
      })

      it.each([
        [
          "English underscore emphasis",
          "hello _world_ there",
          "<p>hello <em>world</em> there</p>",
        ],
        ["CJK asterisk emphasis", "これは*重要*です", "<em>重要</em>"],
        [
          "CJK bold with brackets",
          "日本語**「太字」**テスト",
          "<strong>「太字」</strong>",
        ],
        [
          "complex bold with CJK brackets",
          "本質や内実を隠した、見かけだけの様子。多くの場合、**「中身が伴っていない」「誠実さがない」**という否定的なニュアンスで使われます。",
          "<strong>「中身が伴っていない」「誠実さがない」</strong>",
        ],
        [
          "bold after CJK comma",
          "多くの場合、**「太字」**という",
          "<strong>「太字」</strong>",
        ],
        [
          "italic with CJK punctuation",
          "日本語*「イタリック」*テスト",
          "<em>「イタリック」</em>",
        ],
        [
          "English bold",
          "hello **world** there",
          "<p>hello <strong>world</strong> there</p>",
        ],
      ])("emphasis/bold still works for %s", (_, markdown, expectedSubstring) => {
        expect(toHtml(markdown)).toContain(expectedSubstring)
      })
    })

    it.each([
      ["alphabetical text with space", "hello\nwork", "<p>hello work</p>"],
      ["CJK text without space", "你好\n世界", "<p>你好世界</p>"],
      ["mixed CJK and alphabetical", "hello\n世界", "<p>hello 世界</p>"],
    ])("joins single newlines in %s", (_, markdown, expected) => {
      expect(toHtml(markdown)).toBe(expected)
    })

    describe("<br> handling", () => {
      it("wraps <br> in a <p> tag when surrounded by double newlines", () => {
        const elm = toHtmlElement("hello\n\n<br>\n\nworld")
        const paragraphs = elm.querySelectorAll("p")
        expect(paragraphs.length).toBe(3)
        expect(paragraphs[0]?.textContent).toBe("hello")
        expect(paragraphs[1]?.querySelector("br")).not.toBeNull()
        expect(paragraphs[2]?.textContent).toBe("world")
      })

      it("wraps <br> in a <p> tag after header", () => {
        const elm = toHtmlElement("hello\n=====\n\n<br>\n\nworld")
        expect(elm.querySelector("p br")).not.toBeNull()
        const paragraphs = elm.querySelectorAll("p")
        expect(paragraphs.length).toBe(2)
        expect(paragraphs[0]?.querySelector("br")).not.toBeNull()
        expect(paragraphs[1]?.textContent).toBe("world")
      })

      it("renders multiple consecutive <br> tags as actual line breaks", () => {
        const elm = toHtmlElement("A\n\n<br>\n<br>")
        expect(elm.textContent).not.toContain("<br>")
        expect(elm.querySelector("br")).not.toBeNull()
      })

      it("does not wrap <br> when inside a paragraph", () => {
        const html = toHtml("abc<br>\ndef")
        expect(toHtmlElement("abc<br>\ndef").querySelectorAll("p").length).toBe(
          1
        )
        expect(html).toContain("<p>abc<br")
        expect(html).toContain("def</p>")
        expect(html).not.toMatch(/<\/p><p><br/)
      })

      it("keeps <br> with newline as single paragraph", () => {
        const html = toHtml("hello<br>\nworld")
        expect(
          toHtmlElement("hello<br>\nworld").querySelectorAll("p").length
        ).toBe(1)
        expect(html).not.toMatch(/<br[^>]*>\n/)
      })
    })

    describe("code blocks", () => {
      it("converts to Quill code block HTML", () => {
        expect(toHtml("```\nContent\n```")).toBe(
          '<div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain">Content</div></div>'
        )
      })

      it("converts multi-line to multiple ql-code-block elements", () => {
        const codeBlocks = toHtmlElement(
          "```\nline1\nline2\n```"
        ).querySelectorAll(".ql-code-block")
        expect(codeBlocks.length).toBe(2)
        expect(codeBlocks[0]?.textContent).toBe("line1")
        expect(codeBlocks[1]?.textContent).toBe("line2")
      })

      it("preserves leading spaces", () => {
        expect(
          toHtmlElement("```\n  indented line\n```").querySelector(
            ".ql-code-block"
          )?.textContent
        ).toBe("  indented line")
      })

      it("converts empty line to <br>", () => {
        expect(toHtml("```\n\n```")).toBe(
          '<div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain"><br></div></div>'
        )
      })

      it("escapes HTML tags in content", () => {
        const html = toHtml("```\n<p>X</p>\n```")
        expect(html).toContain("&lt;p&gt;X&lt;/p&gt;")
        expect(
          toHtmlElement("```\n<p>X</p>\n```").querySelector(".ql-code-block")
            ?.textContent
        ).toBe("<p>X</p>")
      })

      it.each([
        [true, "<pre>", "ql-code-block"],
        [false, "ql-code-block-container", "<pre>"],
      ])("with preserve_pre=%s, contains %s and not %s", (preservePre, expected, notExpected) => {
        const html = markdownizer.markdownToHtml("```\ncode content\n```", {
          preserve_pre: preservePre as boolean,
        })
        expect(html).toContain(expected)
        expect(html).toContain("code content")
        expect(html).not.toContain(notExpected)
      })

      it("escapes HTML tags in <pre> when preserve_pre is true", () => {
        const html = markdownizer.markdownToHtml("```\n<p>X</p>\n```", {
          preserve_pre: true,
        })
        expect(html).toContain("&lt;p&gt;X&lt;/p&gt;")
        const div = document.createElement("div")
        div.innerHTML = html
        expect(div.querySelector("pre")?.textContent).toBe("<p>X</p>")
      })
    })
  })

  describe("HTML to markdown", () => {
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

    it("converts HTML code block with blank line to markdown", () => {
      expect(
        markdownizer.htmlToMarkdown("<pre><code>hello\n\nwork\n</code></pre>")
      ).toBeTruthy()
    })

    describe("code block conversions", () => {
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
      ])("converts %s to markdown fenced code block", (_, html, expectedContent) => {
        const markdown = markdownizer.htmlToMarkdown(html)
        expect(markdown).toContain(expectedContent)
        expect(markdown).toMatch(/```[\s\S]*```/)
      })

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
</td></tr><tr><td>
<p class="p1">B</p>
</td><td>
<p class="p1">42</p>
</td></tr><tr><td>
<p class="p1">C</p>
</td><td>
<p class="p1">9</p>
</td></tr><tr><td>
<p class="p1">D</p>
</td><td>
<p class="p1">28</p>
</td></tr></tbody></table>`
      const markdown = markdownizer.htmlToMarkdown(html)
      const headerLine = markdown
        .split("\n")
        .find((line) => line.includes("Item") && line.includes("Value"))
      expect(headerLine).toMatch(/\|.*Item.*\|.*Value.*\|/)
      expect(markdown).toMatch(/\*\*Item\*\*/)
      expect(markdown).toMatch(/\*\*Value\*\*/)
      expect(markdown).toMatch(/\|[\s\S]*-+[\s\S]*\|[\s\S]*-+[\s\S]*\|/)
      expect(markdown).toMatch(/\|[\s\S]*A[\s\S]*\|[\s\S]*17[\s\S]*\|/)
      expect(markdown).toMatch(/\|[\s\S]*B[\s\S]*\|[\s\S]*42[\s\S]*\|/)
      expect(markdown).toMatch(/\|[\s\S]*C[\s\S]*\|[\s\S]*9[\s\S]*\|/)
      expect(markdown).toMatch(/\|[\s\S]*D[\s\S]*\|[\s\S]*28[\s\S]*\|/)
    })
  })
})

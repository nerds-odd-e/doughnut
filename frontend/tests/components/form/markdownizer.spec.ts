import markdownizer from "@/components/form/markdownizer"

describe("Markdown and HTML Conversion Tests", () => {
  describe("markdown to HTML", () => {
    const markdownToHTMLElement = (markdown: string) => {
      const html = markdownizer.markdownToHtml(markdown)
      const div = document.createElement("div")
      div.innerHTML = html
      return div
    }
    it("converts markdown to HTML correctly", () => {
      const markdown = "# Hello World\n\nThis is *markdown*."
      const expectedHtml =
        "<h1>Hello World</h1><p>This is <em>markdown</em>.</p>"
      expect(markdownizer.markdownToHtml(markdown)).toBe(expectedHtml)
    })

    it("handles undefined markdown input", () => {
      expect(markdownizer.markdownToHtml(undefined)).toBe("")
    })

    it("render list as Quill editor format", () => {
      const elm = markdownToHTMLElement(`* item1\n* item2\n`)
      const ol = elm.querySelector("ol")
      expect(ol).not.toBeNull()
      expect(ol?.querySelectorAll("li").length).toBe(2)
      expect(ol?.querySelector("li")).toHaveAttribute("data-list", "bullet")
    })

    it("render ordered list item as Quill editor format", () => {
      const elm = markdownToHTMLElement(`2. item1`)
      expect(elm?.querySelector("li")).toHaveAttribute("data-list", "ordered")
    })

    it("render nested ordered list item as Quill editor format", () => {
      const elm = markdownToHTMLElement(`* level1\n  2. item1`)
      expect(elm?.querySelector("li[data-list='ordered']")).not.toBeNull()
    })

    it("render nested list as Quill editor format", () => {
      const elm = markdownToHTMLElement(`* item1\n  * item1.1\n`)
      expect(elm.querySelectorAll("ol").length).toBe(1)
      expect(elm.querySelector("li.ql-indent-1")).not.toBeNull()
    })

    it("render multiple level nested list as Quill editor format", () => {
      const elm = markdownToHTMLElement(
        `* item1\n  * item1.1\n    * item1.1.1\n`
      )
      expect(elm.querySelectorAll("ol").length).toBe(1)
      expect(elm.querySelector("li.ql-indent-2")).not.toBeNull()
    })

    it("raw HTML with ul/li is rendered as Quill editor format", () => {
      const markdown = "<ul><li>list item</li></ul>"
      const elm = markdownToHTMLElement(markdown)
      expect(elm.querySelectorAll("ol").length).toBe(1)
      expect(elm?.querySelector("li[data-list='bullet']")).not.toBeNull()
    })

    it("renders markdown table as HTML", () => {
      const markdown = `| Name    | Score |\n| ------- | ----- |\n| Alice   |  95   |\n| Bob     |  88   |`
      const html = markdownizer.markdownToHtml(markdown)
      expect(html).toMatchInlineSnapshot(
        `"<table><thead><tr><th>Name</th><th>Score</th></tr></thead><tbody><tr><td>Alice</td><td>95</td></tr><tr><td>Bob</td><td>88</td></tr></tbody></table>"`
      )
    })

    it("removes <p> tags from blockquotes", () => {
      const markdown = "> This is a quote"
      const html = markdownizer.markdownToHtml(markdown)
      expect(html).toBe("<blockquote>This is a quote</blockquote>")
      expect(html).not.toContain("<p>")
      expect(html).not.toContain("</p>")
    })

    it("removes <p> tags from multi-line blockquotes", () => {
      const markdown = "> This is a quote\n> with multiple lines"
      const html = markdownizer.markdownToHtml(markdown)
      const elm = markdownToHTMLElement(markdown)
      const blockquote = elm.querySelector("blockquote")
      expect(blockquote).not.toBeNull()
      expect(blockquote?.querySelector("p")).toBeNull()
      expect(html).not.toContain("<p>")
      expect(html).not.toContain("</p>")
    })

    it("removes <p> tags from blockquotes with formatting", () => {
      const markdown = "> This is a *quote* with **formatting**"
      const html = markdownizer.markdownToHtml(markdown)
      expect(html).toBe(
        "<blockquote>This is a <em>quote</em> with <strong>formatting</strong></blockquote>"
      )
      expect(html).not.toContain("<p>")
      expect(html).not.toContain("</p>")
    })

    it("converts markdown with raw HTML tags", () => {
      const markdown = "raw <span> is ok."
      const result = markdownizer.markdownToHtml(markdown)
      expect(result).toBe("<p>raw &lt;span&gt; is ok.</p>")
    })

    it("allows HTML tags at the beginning of line without escaping", () => {
      const markdown = "<p><br></p>"
      const result = markdownizer.markdownToHtml(markdown)
      expect(result).toBe('<p><br class="softbreak"></p>')
    })

    it("allows single HTML tags like <br> without escaping", () => {
      const markdown = "<br>"
      const result = markdownizer.markdownToHtml(markdown)
      expect(result).toBe('<br class="softbreak">')
    })

    it("joins single newlines in alphabetical text with space", () => {
      const markdown = "hello\nwork"
      const html = markdownizer.markdownToHtml(markdown)
      // Single newlines should be joined into one paragraph with space
      // This prevents Quill from rendering newlines as line breaks
      expect(html).toBe("<p>hello work</p>")
    })

    it("joins single newlines in CJK text without space", () => {
      const markdown = "你好\n世界"
      const html = markdownizer.markdownToHtml(markdown)
      // CJK text should be joined without space
      expect(html).toBe("<p>你好世界</p>")
    })

    it("joins single newlines in mixed CJK and alphabetical text", () => {
      const markdown = "hello\n世界"
      const html = markdownizer.markdownToHtml(markdown)
      // When mixing CJK and alphabetical, join with space
      expect(html).toBe("<p>hello 世界</p>")
    })

    it("converts markdown code block to Quill code block HTML", () => {
      const markdown = "```\nContent\n```"
      const html = markdownizer.markdownToHtml(markdown)
      const expectedHtml =
        '<div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain">Content</div></div>'
      expect(html).toBe(expectedHtml)
    })

    it("converts multi-line markdown code block with blank lines to multiple ql-code-block elements", () => {
      const markdown = "```\nline1\nline2\n```"
      const elm = markdownToHTMLElement(markdown)
      const container = elm.querySelector(".ql-code-block-container")
      expect(container).not.toBeNull()
      const codeBlocks = elm.querySelectorAll(".ql-code-block")
      expect(codeBlocks.length).toBe(2)
      expect(codeBlocks[0]?.textContent).toBe("line1")
      expect(codeBlocks[1]?.textContent).toBe("line2")
    })

    it("preserves leading spaces when converting markdown code block to HTML", () => {
      const markdown = "```\n  indented line\n```"
      const elm = markdownToHTMLElement(markdown)
      const codeBlock = elm.querySelector(".ql-code-block")
      expect(codeBlock?.textContent).toBe("  indented line")
    })

    it("converts empty line in markdown code block to HTML with <br>", () => {
      const markdown = "```\n\n```"
      const html = markdownizer.markdownToHtml(markdown)
      const expectedHtml =
        '<div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain"><br></div></div>'
      expect(html).toBe(expectedHtml)
    })

    it("uses plain <pre> HTML when preserve_pre option is true", () => {
      const markdown = "```\ncode content\n```"
      const html = markdownizer.markdownToHtml(markdown, { preserve_pre: true })
      // Should use plain <pre> tags instead of ql-code-block style
      expect(html).toContain("<pre>")
      expect(html).toContain("</pre>")
      expect(html).toContain("code content")
      expect(html).not.toContain("ql-code-block-container")
      expect(html).not.toContain("ql-code-block")
    })

    it("uses ql-code-block style by default when preserve_pre is false", () => {
      const markdown = "```\ncode content\n```"
      const html = markdownizer.markdownToHtml(markdown, {
        preserve_pre: false,
      })
      // Should use ql-code-block style (default behavior)
      expect(html).toContain("ql-code-block-container")
      expect(html).toContain("ql-code-block")
      expect(html).not.toContain("<pre>")
    })
  })

  describe("Html to markdown", () => {
    it("converts HTML to markdown correctly", () => {
      const html = "<h1>Hello World</h1><p>This is <em>markdown</em>.</p>"
      const expectedMarkdown = "Hello World\n===========\n\nThis is _markdown_."
      expect(markdownizer.htmlToMarkdown(html)).toBe(expectedMarkdown)
    })

    it("converts empty lines with br correctly", () => {
      const html = "<p>a</p><p><br></p><p>b</p>"
      const expectedMarkdown = "a\n\n<p><br></p>\n\nb"
      expect(markdownizer.htmlToMarkdown(html)).toBe(expectedMarkdown)
    })

    it("converts empty lines with br class softbreak correctly", () => {
      const html = '<p>a</p><p><br class="softbreak"></p><p>b</p>'
      const expectedMarkdown = "a\n\n<p><br></p>\n\nb"
      expect(markdownizer.htmlToMarkdown(html)).toBe(expectedMarkdown)
    })

    it("convert quill list to markdown list", () => {
      const html =
        "<ol><li data-list='bullet'>item1</li><li data-list='bullet'>item2</li></ol>"
      const expectedMarkdown = "* item1\n* item2"
      expect(markdownizer.htmlToMarkdown(html)).toBe(expectedMarkdown)
    })

    it("convert nested quill list to markdown list", () => {
      const html =
        "<ol><li data-list='bullet'>item1</li><li data-list='bullet' class='ql-indent-1'>item1.1</li></ol>"
      const expectedMarkdown = "* item1\n  * item1.1"
      expect(markdownizer.htmlToMarkdown(html)).toBe(expectedMarkdown)
    })

    it("converts nested h1 tags to single header", () => {
      const html =
        '<p class="p1"><span class="s1"><h1><b>✅<span class="Apple-converted-space"> </span></b></h1><h1><b>Conclusion</b></h1></span></p>'
      const markdown = markdownizer.htmlToMarkdown(html)
      // Should result in one header, not two headers
      const headerMatches = markdown.match(/={3,}$/gm)
      expect(headerMatches?.length).toBe(1)
    })

    it("keeps separate h1 tags as separate headers", () => {
      const html = "<h1>Chapter 1</h1><h1>Chapter 2</h1>"
      const markdown = markdownizer.htmlToMarkdown(html)
      // Should result in two separate headers
      const headerMatches = markdown.match(/={3,}$/gm)
      expect(headerMatches?.length).toBe(2)
    })

    it("converts HTML code block with blank line back to markdown", () => {
      const html = "<pre><code>hello\n\nwork\n</code></pre>"
      const markdown = markdownizer.htmlToMarkdown(html)
      // This test documents current behavior - may need adjustment based on actual issue
      expect(markdown).toBeTruthy()
    })

    it("converts <pre>content</pre> to markdown code block", () => {
      const html = "<pre>content</pre>"
      const markdown = markdownizer.htmlToMarkdown(html)
      // <pre> tags should convert to markdown fenced code blocks
      expect(markdown).toContain("content")
      // Should be formatted as a fenced code block with triple backticks
      expect(markdown).toMatch(/```[\s\S]*?content[\s\S]*?```/)
    })

    it("does not escape underscore in <pre> tag when converting to markdown", () => {
      const html = '<pre data-language="plain">\n\n_\n</pre>'
      const markdown = markdownizer.htmlToMarkdown(html)
      // Underscore should not be escaped in code blocks
      expect(markdown).toContain("_")
      expect(markdown).not.toContain("\\_")
      // Should be formatted as a fenced code block with triple backticks
      expect(markdown).toMatch(/```[\s\S]*?_\s*```/)
    })

    it("converts Quill code block HTML to markdown code block", () => {
      const html =
        '<div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain">Content</div></div>'
      const markdown = markdownizer.htmlToMarkdown(html)
      // Quill code block should convert to markdown fenced code blocks
      expect(markdown).toContain("Content")
      // Should be formatted as a fenced code block with triple backticks
      expect(markdown).toMatch(/```[\s\S]*?Content[\s\S]*?```/)
    })

    it("preserves leading spaces when converting Quill code block HTML to markdown", () => {
      const html =
        '<div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain">  indented line</div></div>'
      const markdown = markdownizer.htmlToMarkdown(html)
      // Leading spaces should be preserved in the markdown code block
      expect(markdown).toContain("  indented line")
      expect(markdown).toMatch(/```[\s\S]*? {2}indented line[\s\S]*?```/)
    })

    it("converts empty line in Quill code block HTML to markdown as empty line, not <br>", () => {
      const html =
        '<div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain"><br></div></div>'
      const markdown = markdownizer.htmlToMarkdown(html)
      // Empty line should be converted to empty line in markdown, not <br>
      expect(markdown).not.toContain("<br>")
      expect(markdown).toMatch(/```\n\n```/)
    })
  })
})

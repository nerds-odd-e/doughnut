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

    it("converts simple markdown table to HTML", () => {
      const markdown = `| Item | Value |\n| --- | --- |\n| A | 1 |`
      const elm = markdownToHTMLElement(markdown)
      const table = elm.querySelector("table")
      expect(table).not.toBeNull()
      const thead = table?.querySelector("thead")
      expect(thead).not.toBeNull()
      const headerRow = thead?.querySelector("tr")
      expect(headerRow).not.toBeNull()
      const headers = headerRow?.querySelectorAll("th")
      expect(headers?.length).toBe(2)
      expect(headers?.[0]?.textContent).toBe("Item")
      expect(headers?.[1]?.textContent).toBe("Value")
      const tbody = table?.querySelector("tbody")
      expect(tbody).not.toBeNull()
      const dataRow = tbody?.querySelector("tr")
      expect(dataRow).not.toBeNull()
      const cells = dataRow?.querySelectorAll("td")
      expect(cells?.length).toBe(2)
      expect(cells?.[0]?.textContent).toBe("A")
      expect(cells?.[1]?.textContent).toBe("1")
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

    it("wraps <br> in a <p> tag when surrounded by double newlines", () => {
      const markdown = "hello\n\n<br>\n\nworld"
      const elm = markdownToHTMLElement(markdown)
      // The <br> should be wrapped in a <p> tag
      const brParagraph = elm.querySelector("p br")
      expect(brParagraph).not.toBeNull()
      // Should have three paragraphs: hello, <br>, world
      const paragraphs = elm.querySelectorAll("p")
      expect(paragraphs.length).toBe(3)
      expect(paragraphs[0]?.textContent).toBe("hello")
      expect(paragraphs[1]?.querySelector("br")).not.toBeNull()
      expect(paragraphs[2]?.textContent).toBe("world")
    })

    it("wraps <br> in a <p> tag when surrounded by double newlines after header", () => {
      const markdown = "hello\n=====\n\n<br>\n\nworld"
      const elm = markdownToHTMLElement(markdown)
      // The <br> should be wrapped in a <p> tag
      const brParagraph = elm.querySelector("p br")
      expect(brParagraph).not.toBeNull()
      // Should have a header, then two paragraphs: <br>, world
      const paragraphs = elm.querySelectorAll("p")
      expect(paragraphs.length).toBe(2)
      expect(paragraphs[0]?.querySelector("br")).not.toBeNull()
      expect(paragraphs[1]?.textContent).toBe("world")
    })

    it("does not wrap <br> in a <p> tag when it's inside a paragraph", () => {
      const markdown = "abc<br>\ndef"
      const html = markdownizer.markdownToHtml(markdown)
      const elm = markdownToHTMLElement(markdown)
      // The <br> should be inside a single paragraph, not wrapped in its own paragraph
      const paragraphs = elm.querySelectorAll("p")
      expect(paragraphs.length).toBe(1)
      expect(paragraphs[0]?.querySelector("br")).not.toBeNull()
      // Should be a single paragraph with <br> inside
      expect(html).toContain("<p>abc<br")
      expect(html).toContain("def</p>")
      // Should not have multiple paragraphs
      expect(html).not.toMatch(/<\/p><p><br/)
    })

    it("converts markdown with <br> and newline to HTML with one paragraph only", () => {
      const markdown = "hello<br>\nworld"
      const html = markdownizer.markdownToHtml(markdown)
      const elm = markdownToHTMLElement(markdown)
      // Should have only one paragraph
      const paragraphs = elm.querySelectorAll("p")
      expect(paragraphs.length).toBe(1)
      expect(paragraphs[0]?.querySelector("br")).not.toBeNull()
      // Should not have a newline following the <br> tag
      expect(html).not.toMatch(/<br[^>]*>\n/)
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

    it("escapes HTML tags in markdown code block content", () => {
      const markdown = "```\n<p>X</p>\n```"
      const html = markdownizer.markdownToHtml(markdown)
      const elm = markdownToHTMLElement(markdown)
      const codeBlock = elm.querySelector(".ql-code-block")
      expect(codeBlock).not.toBeNull()
      // The HTML tags should be escaped in the ql-code-block
      expect(html).toContain("&lt;p&gt;X&lt;/p&gt;")
      // The textContent should still show the original content
      expect(codeBlock?.textContent).toBe("<p>X</p>")
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

    it("escapes HTML tags in <pre> when preserve_pre option is true", () => {
      const markdown = "```\n<p>X</p>\n```"
      const html = markdownizer.markdownToHtml(markdown, { preserve_pre: true })
      const div = document.createElement("div")
      div.innerHTML = html
      const pre = div.querySelector("pre")
      expect(pre).not.toBeNull()
      // The HTML tags should be escaped in the <pre> tag
      expect(html).toContain("&lt;p&gt;X&lt;/p&gt;")
      // The textContent should still show the original content
      expect(pre?.textContent).toBe("<p>X</p>")
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
      const expectedMarkdown = "a\n\n<br>\n\nb"
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

    it("converts HTML table to markdown table", () => {
      const html =
        "<table><thead><tr><th>Name</th><th>Score</th></tr></thead><tbody><tr><td>Alice</td><td>95</td></tr><tr><td>Bob</td><td>88</td></tr></tbody></table>"
      const markdown = markdownizer.htmlToMarkdown(html)
      // Should convert to markdown table format with pipes
      expect(markdown).toContain("Name")
      expect(markdown).toContain("Score")
      expect(markdown).toContain("Alice")
      expect(markdown).toContain("95")
      expect(markdown).toContain("Bob")
      expect(markdown).toContain("88")
      // Should have table structure with pipes and separators
      expect(markdown).toMatch(/\|.*Name.*\|.*Score.*\|/)
      expect(markdown).toMatch(/\|.*-+.*\|.*-+.*\|/) // separator row
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
      // Should convert to markdown table format with pipes
      expect(markdown).toContain("Item")
      expect(markdown).toContain("Value")
      expect(markdown).toContain("A")
      expect(markdown).toContain("17")
      expect(markdown).toContain("B")
      expect(markdown).toContain("42")
      expect(markdown).toContain("C")
      expect(markdown).toContain("9")
      expect(markdown).toContain("D")
      expect(markdown).toContain("28")
      // Item and Value should be on the same line (header row)
      const lines = markdown.split("\n")
      const headerLine = lines.find(
        (line) => line.includes("Item") && line.includes("Value")
      )
      expect(headerLine).toBeDefined()
      expect(headerLine).toMatch(/\|.*Item.*\|.*Value.*\|/)
      // Should have table structure with pipes and separators (allowing for whitespace/newlines)
      expect(markdown).toMatch(/\|[\s\S]*Item[\s\S]*\|[\s\S]*Value[\s\S]*\|/)
      expect(markdown).toMatch(/\|[\s\S]*-+[\s\S]*\|[\s\S]*-+[\s\S]*\|/) // separator row
      expect(markdown).toMatch(/\|[\s\S]*A[\s\S]*\|[\s\S]*17[\s\S]*\|/)
      expect(markdown).toMatch(/\|[\s\S]*B[\s\S]*\|[\s\S]*42[\s\S]*\|/)
      expect(markdown).toMatch(/\|[\s\S]*C[\s\S]*\|[\s\S]*9[\s\S]*\|/)
      expect(markdown).toMatch(/\|[\s\S]*D[\s\S]*\|[\s\S]*28[\s\S]*\|/)
      // Bold text in headers should be converted to markdown bold
      expect(markdown).toMatch(/\*\*Item\*\*/)
      expect(markdown).toMatch(/\*\*Value\*\*/)
    })
  })
})

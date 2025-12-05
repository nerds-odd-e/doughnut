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

    it("converts <pre><code> blocks to markdown code blocks", () => {
      const html =
        "<pre><code>&lt;p class=&quot;p1&quot;&gt;\n  &lt;span class=&quot;s1&quot;&gt;\n    &lt;h1&gt;&lt;b&gt;✅&lt;span class=&quot;Apple-converted-space&quot;&gt; &lt;/span&gt;&lt;/b&gt;&lt;/h1&gt;\n    &lt;h1&gt;&lt;b&gt;Conclusion&lt;/b&gt;&lt;/h1&gt;\n  &lt;/span&gt;\n&lt;/p&gt;</code></pre>"
      const markdown = markdownizer.htmlToMarkdown(html)
      // Should contain markdown code block with ```
      expect(markdown).toContain("```")
      expect(markdown).toContain('<p class="p1">')
      expect(markdown).toContain("<h1><b>✅")
      expect(markdown).toContain("<h1><b>Conclusion</b></h1>")
    })

    it("converts markdown code blocks back to <pre><code>", () => {
      const markdown =
        '```\n<p class="p1">\n  <span class="s1">\n    <h1><b>✅<span class="Apple-converted-space"> </span></b></h1>\n    <h1><b>Conclusion</b></h1>\n  </span>\n</p>\n```'
      const html = markdownizer.markdownToHtml(markdown)
      // Should contain <pre><code>
      expect(html).toContain("<pre><code>")
      expect(html).toContain("</code></pre>")
      // HTML entities should be escaped
      expect(html).toContain("&lt;p")
      expect(html).toContain("&quot;")
    })

    it("round-trip conversion: <pre><code> to markdown and back", () => {
      const originalHtml =
        "<pre><code>&lt;p class=&quot;p1&quot;&gt;\n  &lt;span class=&quot;s1&quot;&gt;\n    &lt;h1&gt;&lt;b&gt;✅&lt;span class=&quot;Apple-converted-space&quot;&gt; &lt;/span&gt;&lt;/b&gt;&lt;/h1&gt;\n    &lt;h1&gt;&lt;b&gt;Conclusion&lt;/b&gt;&lt;/h1&gt;\n  &lt;/span&gt;\n&lt;/p&gt;</code></pre>"
      const markdown = markdownizer.htmlToMarkdown(originalHtml)
      const convertedHtml = markdownizer.markdownToHtml(markdown)
      // Should contain <pre><code> in the result
      expect(convertedHtml).toContain("<pre><code>")
      expect(convertedHtml).toContain("</code></pre>")
    })
  })
})

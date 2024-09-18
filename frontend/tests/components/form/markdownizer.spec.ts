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

    it("render nested list as Quill editor format", () => {
      const elm = markdownToHTMLElement(`* item1\n  * item1.1\n`)
      expect(elm.querySelectorAll("ol").length).toBe(1)
      expect(elm.querySelector("li.ql-indent-1")).not.toBeNull()
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
  })
})

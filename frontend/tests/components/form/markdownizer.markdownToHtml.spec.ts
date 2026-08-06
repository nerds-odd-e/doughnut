import markdownizer from "@/components/form/markdownizer"
import { describe, it, expect } from "vitest"
import { toHtml, toHtmlElement } from "./markdownizerTestSupport"

describe("markdownizer markdownToHtml", () => {
  describe("round-trip conversion (pasted HTML -> markdown -> HTML)", () => {
    it("wraps standalone br in paragraph when before list", () => {
      const pastedHtml = `<p><br></p><ul><li>Item 1</li><li>Item 2</li></ul>`
      const markdown = markdownizer.htmlToMarkdown(pastedHtml)
      const html = markdownizer.markdownToHtml(markdown, {
        preserve_pre: true,
      })
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
      [
        "inline HTML strong tag",
        "abc <strong>def</strong>",
        "<p>abc <strong>def</strong></p>",
      ],
      [
        "inline HTML strong with CJK",
        "你很<strong>好吃</strong>",
        "<p>你很<strong>好吃</strong></p>",
      ],
      [
        "inline HTML mark tag",
        "abc <mark>highlighted</mark>",
        "<p>abc <mark>highlighted</mark></p>",
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
  })
})

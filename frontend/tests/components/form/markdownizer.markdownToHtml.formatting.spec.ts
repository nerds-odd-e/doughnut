import { describe, it, expect } from "vitest"
import { toHtml, toHtmlElement } from "./markdownizerTestSupport"

describe("markdownizer markdownToHtml formatting", () => {
  describe("CJK underscore handling", () => {
    it.each([
      [
        "adjacent to CJK characters",
        "これは_重要_なことです",
        "<p>これは_重要_なことです</p>",
      ],
      ["after CJK opening bracket", "「_水曜日_」", "<p>「_水曜日_」</p>"],
      ["after Japanese period", "日本語。_日本語_", "<p>日本語。_日本語_</p>"],
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
      expect(toHtml(markdown)).toBe(expected)
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
      const elm = toHtmlElement("abc<br>\ndef")
      const html = elm.innerHTML
      expect(elm.querySelectorAll("p").length).toBe(1)
      expect(html).toContain("<p>abc<br")
      expect(html).toContain("def</p>")
      expect(html).not.toMatch(/<\/p><p><br/)
    })

    it("keeps <br> with newline as single paragraph", () => {
      const elm = toHtmlElement("hello<br>\nworld")
      const html = elm.innerHTML
      expect(elm.querySelectorAll("p").length).toBe(1)
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
      const elm = toHtmlElement("```\n<p>X</p>\n```")
      expect(elm.innerHTML).toContain("&lt;p&gt;X&lt;/p&gt;")
      expect(elm.querySelector(".ql-code-block")?.textContent).toBe("<p>X</p>")
    })

    it.each([
      [true, "<pre>", "ql-code-block"],
      [false, "ql-code-block-container", "<pre>"],
    ])(
      "with preserve_pre=%s, contains %s and not %s",
      (preservePre, expected, notExpected) => {
        const html = toHtml("```\ncode content\n```", {
          preserve_pre: preservePre as boolean,
        })
        expect(html).toContain(expected)
        expect(html).toContain("code content")
        expect(html).not.toContain(notExpected)
      }
    )

    it("escapes HTML tags in <pre> when preserve_pre is true", () => {
      const elm = toHtmlElement("```\n<p>X</p>\n```", { preserve_pre: true })
      expect(elm.innerHTML).toContain("&lt;p&gt;X&lt;/p&gt;")
      expect(elm.querySelector("pre")?.textContent).toBe("<p>X</p>")
    })
  })
})

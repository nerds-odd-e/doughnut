import { describe, it, expect } from "vitest"
import htmlToMarkdown from "@/components/form/quillHtmlToMarkdown"

describe("quillHtmlToMarkdown", () => {
  it("converts HTML with escaped angle brackets", () => {
    const html = "<p>raw &lt;span&gt; is ok.</p>"
    const result = htmlToMarkdown(html)
    expect(result).toBe("raw <span> is ok.")
  })

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
})

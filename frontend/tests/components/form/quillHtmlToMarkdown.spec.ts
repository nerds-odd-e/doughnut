import { describe, it, expect } from "vitest"
import htmlToMarkdown from "@/components/form/quillHtmlToMarkdown"

describe("quillHtmlToMarkdown", () => {
  it("converts HTML with escaped angle brackets", () => {
    const html = "<p>raw &lt;span&gt; is ok.</p>"
    const result = htmlToMarkdown(html)
    expect(result).toBe("raw <span> is ok.")
  })

  it("converts HTML with soft break", () => {
    const html = '<p>Hello<br class="softbreak">World</p>'
    const result = htmlToMarkdown(html)
    expect(result).toBe("Hello<br>\nWorld")
  })
})

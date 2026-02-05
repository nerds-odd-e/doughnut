import { describe, it, expect } from "vitest"
import htmlToMarkdown from "@/components/form/quillHtmlToMarkdown"
import markdownizer from "@/components/form/markdownizer"

describe("quillHtmlToMarkdown", () => {
  it("does not insert br after first bullet when empty paragraph precedes list", () => {
    // This HTML is typical of content pasted from ChatGPT or similar applications
    // It has an empty paragraph with <br> before the list
    const html = `<p style="margin: 0.0px 0.0px 12.0px 0.0px; font: 12.0px 'Times New Roman'; -webkit-text-stroke: #000000; min-height: 13.8px"><span style="font-family: 'Times New Roman'; font-weight: normal; font-style: normal; font-size: 12.00px; font-kerning: none"></span><br></p>
<ul style="list-style-type: disc">
  <li style="margin: 0.0px 0.0px 12.0px 0.0px; font: 12.0px 'Times New Roman'; -webkit-text-stroke: #000000"><span style="font-family: 'Times New Roman'; font-weight: normal; font-style: normal; font-size: 12.00px; font-kerning: none">No rendaku</span></li>
  <li style="margin: 0.0px 0.0px 12.0px 0.0px; font: 12.0px 'Times New Roman'; -webkit-text-stroke: #000000"><span style="font-family: 'Times New Roman'; font-weight: normal; font-style: normal; font-size: 12.00px; font-kerning: none">No dakuten</span></li>
</ul>
<p style="margin: 0.0px 0.0px 12.0px 0.0px; font: 12.0px 'Times New Roman'; -webkit-text-stroke: #000000; min-height: 13.8px"><span style="font-family: 'Times New Roman'; font-weight: normal; font-style: normal; font-size: 12.00px; font-kerning: none"></span><br></p>
<br class="Apple-interchange-newline">`
    const result = htmlToMarkdown(html)
    // The first bullet should not have a <br> after it
    // Expected: "- No rendaku" or "* No rendaku", NOT "- <br>\nNo rendaku"
    expect(result).not.toMatch(/[-*]\s*<br>\s*\n/)
    // Both items should be proper list items
    expect(result).toContain("No rendaku")
    expect(result).toContain("No dakuten")
  })

  it("should produce markdown where br is followed by blank line before list", () => {
    // Test that the markdown has a blank line between <br> and the list
    const html = `<p><br></p><ul><li>Item 1</li><li>Item 2</li></ul>`
    const markdown = htmlToMarkdown(html)

    // The <br> should be followed by two newlines (blank line) before the list
    // This ensures the list is properly separated from the <br>
    expect(markdown).toMatch(/<br>\n\n[-*]/)
  })

  it("full round-trip should not put br inside list item", () => {
    // Simulates the exact paste flow in QuillEditor
    const pastedHtml = `<p><br></p><ul><li>Item 1</li><li>Item 2</li></ul>`

    // Step 1: Convert HTML to markdown
    const markdown = markdownizer.htmlToMarkdown(pastedHtml)

    // Step 2: Convert markdown to HTML (with preserve_pre: true, like in QuillEditor)
    const outputHtml = markdownizer.markdownToHtml(markdown, {
      preserve_pre: true,
    })

    // Parse the output HTML
    const div = document.createElement("div")
    div.innerHTML = outputHtml

    // The first list item should NOT start with <br>
    const firstLi = div.querySelector("li")
    expect(firstLi).not.toBeNull()

    // Check that the first child of the list item is not a <br>
    if (firstLi?.firstChild) {
      // If it's a text node, it should be the item text
      if (firstLi.firstChild.nodeType === Node.TEXT_NODE) {
        expect(firstLi.firstChild.textContent?.trim()).toBe("Item 1")
      } else if (firstLi.firstChild.nodeType === Node.ELEMENT_NODE) {
        // If it's an element, it should not be a <br>
        expect((firstLi.firstChild as Element).tagName).not.toBe("BR")
      }
    }

    // The list item text content should be exactly the item text
    expect(firstLi?.textContent?.trim()).toBe("Item 1")
  })

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

  it("converts HTML with code blocks separated by hr to separate markdown code blocks", () => {
    const html =
      '<div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain">A</div></div><p><hr></p><div class="ql-code-block-container" spellcheck="false"><div class="ql-code-block" data-language="plain">B</div></div>'
    const result = htmlToMarkdown(html)
    // Should contain a code block with A only
    expect(result).toContain("```\nA\n```")
    // Should contain a code block with B only
    expect(result).toContain("```\nB\n```")
    // Should not merge A and B into a single code block
    expect(result).not.toContain("```\nA\nB\n```")
  })
})

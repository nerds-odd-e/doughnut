import { describe, it, expect, beforeEach, afterEach, vi } from "vitest"
import { useInterruptingHtmlToMarkdown } from "@/composables/useInterruptingHtmlToMarkdown"

describe("useInterruptingHtmlToMarkdown", () => {
  let originalConfirm: typeof window.confirm

  beforeEach(() => {
    originalConfirm = window.confirm
  })

  afterEach(() => {
    window.confirm = originalConfirm
    vi.restoreAllMocks()
  })

  it("converts HTML to markdown without links", () => {
    const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()
    const html = "<p>Hello World</p>"
    const result = htmlToMarkdown(html)
    expect(result).toContain("Hello World")
  })

  it("converts HTML to markdown with 1 link without prompting", () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false)
    const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()
    const html = '<p>Check out <a href="https://example.com">this link</a></p>'
    const result = htmlToMarkdown(html)
    expect(confirmSpy).not.toHaveBeenCalled()
    expect(result).toMatch(/\[this link\]\(https:\/\/example\.com\)/)
  })

  it("converts HTML to markdown with 2 links without prompting", () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false)
    const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()
    const html =
      '<p>Check <a href="https://example.com">link1</a> and <a href="https://example2.com">link2</a></p>'
    const result = htmlToMarkdown(html)
    expect(confirmSpy).not.toHaveBeenCalled()
    expect(result).toMatch(/\[link1\]\(https:\/\/example\.com\)/)
    expect(result).toMatch(/\[link2\]\(https:\/\/example2\.com\)/)
  })

  it("prompts user when markdown contains more than 2 links", () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false)
    const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()
    const html =
      '<p><a href="https://example.com">link1</a> <a href="https://example2.com">link2</a> <a href="https://example3.com">link3</a></p>'
    htmlToMarkdown(html)
    expect(confirmSpy).toHaveBeenCalledWith(
      "Shall I remove the 3 links from the pasting content?"
    )
  })

  it("removes links when user confirms", () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true)
    const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()
    const html =
      '<p><a href="https://example.com">link1</a> <a href="https://example2.com">link2</a> <a href="https://example3.com">link3</a></p>'
    const result = htmlToMarkdown(html)
    expect(confirmSpy).toHaveBeenCalledWith(
      "Shall I remove the 3 links from the pasting content?"
    )
    expect(result).toContain("link1")
    expect(result).toContain("link2")
    expect(result).toContain("link3")
    expect(result).not.toMatch(/\[link1\]\(https:\/\/example\.com\)/)
    expect(result).not.toMatch(/\[link2\]\(https:\/\/example2\.com\)/)
    expect(result).not.toMatch(/\[link3\]\(https:\/\/example3\.com\)/)
  })

  it("keeps links when user cancels", () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false)
    const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()
    const html =
      '<p><a href="https://example.com">link1</a> <a href="https://example2.com">link2</a> <a href="https://example3.com">link3</a></p>'
    const result = htmlToMarkdown(html)
    expect(confirmSpy).toHaveBeenCalledWith(
      "Shall I remove the 3 links from the pasting content?"
    )
    expect(result).toMatch(/\[link1\]\(https:\/\/example\.com\)/)
    expect(result).toMatch(/\[link2\]\(https:\/\/example2\.com\)/)
    expect(result).toMatch(/\[link3\]\(https:\/\/example3\.com\)/)
  })

  it("handles exactly 3 links correctly", () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true)
    const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()
    const html =
      '<p><a href="https://example.com">one</a> <a href="https://example2.com">two</a> <a href="https://example3.com">three</a></p>'
    const result = htmlToMarkdown(html)
    expect(confirmSpy).toHaveBeenCalledWith(
      "Shall I remove the 3 links from the pasting content?"
    )
    expect(result).toContain("one")
    expect(result).toContain("two")
    expect(result).toContain("three")
    expect(result).not.toMatch(/\[one\]\(https:\/\/example\.com\)/)
  })

  it("handles many links correctly", () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true)
    const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()
    const html =
      '<p><a href="https://example.com">link1</a> <a href="https://example2.com">link2</a> <a href="https://example3.com">link3</a> <a href="https://example4.com">link4</a> <a href="https://example5.com">link5</a></p>'
    const result = htmlToMarkdown(html)
    expect(confirmSpy).toHaveBeenCalledWith(
      "Shall I remove the 5 links from the pasting content?"
    )
    expect(result).toContain("link1")
    expect(result).toContain("link2")
    expect(result).toContain("link3")
    expect(result).toContain("link4")
    expect(result).toContain("link5")
    expect(result).not.toMatch(/\[link1\]\(https:\/\/example\.com\)/)
  })

  it("prompts user when markdown contains more than 2 images", () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false)
    const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()
    const html =
      '<p><img src="https://example.com/img1.jpg" alt="img1"> <img src="https://example.com/img2.jpg" alt="img2"> <img src="https://example.com/img3.jpg" alt="img3"></p>'
    htmlToMarkdown(html)
    expect(confirmSpy).toHaveBeenCalledWith(
      "Shall I remove the 3 images from the pasting content?"
    )
  })

  it("removes images when user confirms", () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true)
    const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()
    const html =
      '<p><img src="https://example.com/img1.jpg" alt="img1"> <img src="https://example.com/img2.jpg" alt="img2"> <img src="https://example.com/img3.jpg" alt="img3"></p>'
    const result = htmlToMarkdown(html)
    expect(confirmSpy).toHaveBeenCalledWith(
      "Shall I remove the 3 images from the pasting content?"
    )
    expect(result).toContain("img1")
    expect(result).toContain("img2")
    expect(result).toContain("img3")
    expect(result).not.toMatch(/!\[img1\]\(https:\/\/example\.com\/img1\.jpg\)/)
    expect(result).not.toMatch(/!\[img2\]\(https:\/\/example\.com\/img2\.jpg\)/)
    expect(result).not.toMatch(/!\[img3\]\(https:\/\/example\.com\/img3\.jpg\)/)
  })

  it("prompts separately for links and images when both exceed threshold", () => {
    const confirmSpy = vi
      .spyOn(window, "confirm")
      .mockReturnValueOnce(true)
      .mockReturnValueOnce(false)
    const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()
    const html =
      '<p><a href="https://example.com">link1</a> <a href="https://example2.com">link2</a> <a href="https://example3.com">link3</a> <img src="https://example.com/img1.jpg" alt="img1"> <img src="https://example.com/img2.jpg" alt="img2"> <img src="https://example.com/img3.jpg" alt="img3"></p>'
    const result = htmlToMarkdown(html)
    expect(confirmSpy).toHaveBeenCalledTimes(2)
    expect(confirmSpy).toHaveBeenNthCalledWith(
      1,
      "Shall I remove the 3 links from the pasting content?"
    )
    expect(confirmSpy).toHaveBeenNthCalledWith(
      2,
      "Shall I remove the 3 images from the pasting content?"
    )
    // Links should be removed (confirmed), images should remain (cancelled)
    expect(result).toContain("link1")
    expect(result).not.toMatch(/\[link1\]\(https:\/\/example\.com\)/)
    expect(result).toMatch(/!\[img1\]\(https:\/\/example\.com\/img1\.jpg\)/)
  })

  it("removes both links and images when both are confirmed", () => {
    const confirmSpy = vi
      .spyOn(window, "confirm")
      .mockReturnValueOnce(true)
      .mockReturnValueOnce(true)
    const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()
    const html =
      '<p><a href="https://example.com">link1</a> <a href="https://example2.com">link2</a> <a href="https://example3.com">link3</a> <img src="https://example.com/img1.jpg" alt="img1"> <img src="https://example.com/img2.jpg" alt="img2"> <img src="https://example.com/img3.jpg" alt="img3"></p>'
    const result = htmlToMarkdown(html)
    expect(confirmSpy).toHaveBeenCalledTimes(2)
    expect(result).toContain("link1")
    expect(result).toContain("img1")
    expect(result).not.toMatch(/\[link1\]\(https:\/\/example\.com\)/)
    expect(result).not.toMatch(/!\[img1\]\(https:\/\/example\.com\/img1\.jpg\)/)
  })
})

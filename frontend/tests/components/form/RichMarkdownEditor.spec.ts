import { noteShowHref } from "@/routes/noteShowLocation"
import { wikiTitleFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { nextTick } from "vue"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("does not emit update:modelValue on mount", async () => {
    await h.mountEditor("initial value")
    expect(h.getWrapper().emitted()["update:modelValue"]).toBeUndefined()
    await h.mountEditor("# Title", { readonly: true })
    expect(h.getWrapper().emitted()["update:modelValue"]).toBeUndefined()
  })

  it("prompts for readme content when empty in readme context", async () => {
    await h.mountEditor("", { isReadmeContext: true, attachToBody: true })
    expect(h.quillEditorEl().getAttribute("data-placeholder")).toBe(
      "Enter readme content here..."
    )
  })

  it("converts pasted HTML to markdown", async () => {
    await h.mountEditor("", { attachToBody: true })
    await h.dispatchPasteHtmlToQuill("<p><strong>Bold text</strong></p>")
    expect(h.lastEmittedMarkdown()).toContain("Bold text")
  })

  it("preserves nested bullet indentation when pasting ChatGPT-style HTML", async () => {
    await h.mountEditor("", { attachToBody: true })
    await h.dispatchPasteHtmlToQuill(
      `<p class="p1">Intro</p><ul><li><span class="s1"><b>Japan</b></span><ul><li>correct: circle</li><li>incorrect: cross</li></ul></li></ul>`
    )
    const markdown = h.lastEmittedMarkdown()
    expect(markdown).toMatch(/\n {2,}\* +correct: circle/)
    expect(markdown).toMatch(/\n {2,}\* +incorrect: cross/)
    expect(
      Array.from(h.quillEditorEl().querySelectorAll("li")).map((li) => ({
        text: li.textContent?.trim(),
        indent: li.className.match(/ql-indent-(\d+)/)?.[1] ?? "0",
      }))
    ).toEqual([
      { text: "Japan", indent: "0" },
      { text: "correct: circle", indent: "1" },
      { text: "incorrect: cross", indent: "1" },
    ])
  })

  it("does not paste when readonly", async () => {
    await h.mountEditor("", { readonly: true })
    await h.dispatchPasteHtmlToQuill("<p>Test</p>")
    expect(h.getWrapper().emitted()["update:modelValue"]).toBeUndefined()
  })

  it("linkifies wikilinks in Quill HTML while model matches the interval", async () => {
    const wikiTitles = [wikiTitleFromAuthoredToken("MyNote", 42)]
    const wrapper = await h.mountEditor("", { wikiTitles })
    h.emitQuillModelValue("<p>[[MyNote]]</p>")
    await wrapper.setProps({ modelValue: "[[MyNote]]" })
    await nextTick()

    expect(h.quillModelHtml()).toContain(
      `<a href="${noteShowHref(42)}" class="donut-wiki-link" data-wiki-title="MyNote" data-note-id="42"`
    )
  })

  it("keeps canonical dead-wiki-link HTML identical to Quill internal HTML", async () => {
    await h.mountEditor("[[Missing Note]]")
    const translatedHtml = h.quillModelHtml()

    expect(translatedHtml).toContain('class="dead-wiki-link"')
    expect(h.quillEditorEl().innerHTML).toBe(translatedHtml)
  })

  it("keeps active Quill HTML identical when saved markdown with a dead link is echoed back", async () => {
    const wrapper = await h.mountEditor("Hello")
    h.emitQuillModelValue("<p>Hello [[Missing Note]]</p>")
    await wrapper.setProps({ modelValue: "Hello [[Missing Note]]" })
    await nextTick()

    expect(h.quillModelHtml()).toContain('class="dead-wiki-link"')
    expect(h.quillModelHtml()).toBe(h.quillEditorEl().innerHTML)
  })
})

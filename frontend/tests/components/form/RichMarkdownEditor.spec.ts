import { wikiTitleFromInnerAndNoteId } from "@/utils/wikiPropertyValueField"
import { nextTick } from "vue"
import type { VueWrapper } from "@vue/test-utils"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor", () => {
  const h = createRichMarkdownEditorTestHarness()

  function quillFrom(wrapper: VueWrapper) {
    return wrapper.findComponent({ name: "QuillEditor" })
  }

  afterEach(() => {
    h.cleanup()
  })

  it("does not emit update:modelValue on mount", async () => {
    await h.mountEditor("initial value")
    expect(h.getWrapper().emitted()["update:modelValue"]).toBeUndefined()
    await h.mountEditor("# Title", { readonly: true })
    expect(h.getWrapper().emitted()["update:modelValue"]).toBeUndefined()
  })

  it("converts pasted HTML to markdown", async () => {
    await h.mountEditor("", { attachToBody: true })
    await h.dispatchPasteHtmlToQuill("<p><strong>Bold text</strong></p>")
    const emitted = h.getWrapper().emitted()["update:modelValue"]
    expect(emitted?.length).toBeGreaterThan(0)
    expect(emitted![emitted!.length - 1]![0]).toContain("Bold text")
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
    const wikiTitles = [wikiTitleFromInnerAndNoteId("MyNote", 42)]
    const wrapper = await h.mountEditor("", { wikiTitles })
    const quill = quillFrom(wrapper)
    quill.vm.$emit("update:modelValue", "<p>[[MyNote]]</p>")
    await wrapper.setProps({ modelValue: "[[MyNote]]" })
    await nextTick()

    expect(String(quill.props("modelValue"))).toContain(
      '<a href="/n42" class="doughnut-link" data-wiki-title="MyNote"'
    )
  })

  it("keeps canonical dead-link HTML identical to Quill internal HTML", async () => {
    const wrapper = await h.mountEditor("[[Missing Note]]")
    const quill = quillFrom(wrapper)
    const translatedHtml = String(quill.props("modelValue"))

    expect(translatedHtml).toContain('class="dead-link"')
    expect(h.quillEditorEl().innerHTML).toBe(translatedHtml)
  })

  it("keeps active Quill HTML identical when saved markdown with a dead link is echoed back", async () => {
    const wrapper = await h.mountEditor("Hello")
    const quill = quillFrom(wrapper)
    quill.vm.$emit("update:modelValue", "<p>Hello [[Missing Note]]</p>")
    await wrapper.setProps({ modelValue: "Hello [[Missing Note]]" })
    await nextTick()

    expect(String(quill.props("modelValue"))).toContain('class="dead-link"')
    expect(quill.props("modelValue")).toBe(h.quillEditorEl().innerHTML)
  })
})

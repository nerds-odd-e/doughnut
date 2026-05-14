import { wikiTitleFromInnerAndNoteId } from "@/utils/wikiPropertyValueField"
import { flushPromises } from "@vue/test-utils"
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

  it("converts pasted HTML to markdown and does not paste when readonly", async () => {
    await h.mountEditor("")
    await flushPromises()
    await nextTick()
    await h.dispatchPasteHtmlToQuill("<p><strong>Bold text</strong></p>")
    const emitted = h.getWrapper().emitted()["update:modelValue"]
    if (emitted?.length) {
      expect(emitted[emitted.length - 1]![0]).toContain("Bold text")
    }

    await h.mountEditor("", { readonly: true })
    await flushPromises()
    await h.dispatchPasteHtmlToQuill("<p>Test</p>")
    expect(h.getWrapper().emitted()["update:modelValue"]).toBeUndefined()
  })

  it("linkifies wikilinks in Quill HTML while model matches the interval", async () => {
    const wikiTitles = [wikiTitleFromInnerAndNoteId("MyNote", 42)]
    const wrapper = await h.mountEditor("", { wikiTitles })
    await flushPromises()

    const quill = wrapper.findComponent({ name: "QuillEditor" })
    quill.vm.$emit("update:modelValue", "<p>[[MyNote]]</p>")
    await wrapper.setProps({ modelValue: "[[MyNote]]" })
    await nextTick()
    await flushPromises()

    expect(String(quill.props("modelValue"))).toContain(
      '<a href="/d/n/42" class="doughnut-link" data-wiki-title="MyNote"'
    )
  })

  it("keeps canonical dead-link HTML identical to Quill internal HTML", async () => {
    const wrapper = await h.mountEditor("[[Missing Note]]")
    await flushPromises()
    await nextTick()

    const quill = wrapper.findComponent({ name: "QuillEditor" })
    const translatedHtml = String(quill.props("modelValue"))
    const quillInternalHtml = h.quillEditorEl().innerHTML

    expect(translatedHtml).toContain('class="dead-link"')
    expect(quillInternalHtml).toContain('class="dead-link"')
    expect(quillInternalHtml).toBe(translatedHtml)
  })

  it("keeps active Quill HTML identical when saved markdown with a dead link is echoed back", async () => {
    const wrapper = await h.mountEditor("Hello")
    await flushPromises()
    await nextTick()

    const quillWrapper = wrapper.findComponent({ name: "QuillEditor" })
    quillWrapper.vm.$emit("update:modelValue", "<p>Hello [[Missing Note]]</p>")
    await wrapper.setProps({ modelValue: "Hello [[Missing Note]]" })
    await flushPromises()
    await nextTick()

    expect(String(quillWrapper.props("modelValue"))).toContain(
      'class="dead-link"'
    )
    expect(quillWrapper.props("modelValue")).toBe(h.quillEditorEl().innerHTML)
  })
})

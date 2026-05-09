import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"
import helper from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { nextTick } from "vue"

export function createRichMarkdownEditorTestHarness() {
  let wrapper: VueWrapper

  async function setWikiPropertyValueField(
    field: ReturnType<VueWrapper["find"]>,
    text: string
  ) {
    const el = field.element as HTMLElement
    el.textContent = text
    await field.trigger("input")
    await flushPromises()
  }

  function lastEmittedMarkdown(): string {
    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted?.length).toBeGreaterThan(0)
    return emitted![emitted!.length - 1]![0] as string
  }

  function quillEditorEl(): HTMLElement {
    return wrapper
      .findComponent({ name: "QuillEditor" })
      .vm.$el.querySelector(".ql-editor") as HTMLElement
  }

  async function dispatchPasteHtmlToQuill(html: string) {
    const qlEditor = quillEditorEl()
    qlEditor.focus()
    await nextTick()
    await flushPromises()
    const clipboardData = new DataTransfer()
    clipboardData.setData("text/html", html)
    qlEditor.dispatchEvent(
      new ClipboardEvent("paste", {
        bubbles: true,
        cancelable: true,
        clipboardData,
      })
    )
    await nextTick()
    await flushPromises()
  }

  async function openAddProperty() {
    const addBtn = wrapper
      .findAll("button")
      .find((w) => w.text().includes("Add property"))
    expect(addBtn).toBeDefined()
    await addBtn!.trigger("click")
    await flushPromises()
  }

  async function flushAnimationFrame() {
    await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
  }

  async function assertPresetOptionsVisible(expectedKeys: readonly string[]) {
    const options = wrapper.findAll(
      '[data-testid="rich-note-property-key-preset-option"]'
    )
    expect(options.length).toBe(expectedKeys.length)
    for (const key of expectedKeys) {
      expect(
        options.find(
          (o) => (o.element as HTMLElement).dataset.presetKey === key
        )
      ).toBeDefined()
    }
  }

  async function selectPresetKey(presetKey: string) {
    const btn = wrapper
      .findAll('[data-testid="rich-note-property-key-preset-option"]')
      .find((w) => (w.element as HTMLElement).dataset.presetKey === presetKey)
    expect(btn).toBeDefined()
    await btn!.trigger("mousedown")
    await btn!.trigger("click")
    await flushPromises()
  }

  async function mountEditor(initialValue: string, options = {}) {
    wrapper = helper
      .component(RichMarkdownEditor)
      .withRouter()
      .withProps({
        modelValue: initialValue,
        wikiTitles: [],
        ...options,
      })
      .mount({ attachTo: document.body })
    await flushPromises()
    return wrapper
  }

  function cleanup() {
    wrapper?.unmount()
    document.body.innerHTML = ""
  }

  return {
    mountEditor,
    /** Same instance as the last `mountEditor` return value. */
    getWrapper: () => wrapper,
    cleanup,
    setWikiPropertyValueField,
    lastEmittedMarkdown,
    quillEditorEl,
    dispatchPasteHtmlToQuill,
    openAddProperty,
    flushAnimationFrame,
    assertPresetOptionsVisible,
    selectPresetKey,
  }
}

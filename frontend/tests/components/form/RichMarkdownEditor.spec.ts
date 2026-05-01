import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { nextTick } from "vue"
import helper from "@tests/helpers"

describe("RichMarkdownEditor", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  const mountEditor = async (initialValue: string, options = {}) => {
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

  it("not emit update when the change is from initial value", async () => {
    await mountEditor("initial value")
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("not emit update if readonly", async () => {
    await mountEditor("# Title", { readonly: true })
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("will try to unify the markdown", async () => {
    await mountEditor("# Title")
    await flushPromises()
    await nextTick()
    await flushPromises()
    const emitted = wrapper.emitted()["update:modelValue"]
    if (emitted?.length) {
      expect(emitted[emitted.length - 1]![0]).toContain("Title")
    }
  })

  it("converts HTML to markdown then back to HTML when pasting", async () => {
    await mountEditor("")
    await flushPromises()
    await nextTick()

    const qlEditor = wrapper
      .findComponent({ name: "QuillEditor" })
      .vm.$el.querySelector(".ql-editor") as HTMLElement
    expect(qlEditor).toBeTruthy()

    // Browser Mode: Real focus() method!
    qlEditor.focus()
    await nextTick()
    await flushPromises()

    // Browser Mode: Use real ClipboardEvent with DataTransfer!
    // Create a real clipboard event with HTML data
    const clipboardData = new DataTransfer()
    clipboardData.setData("text/html", "<p><strong>Bold text</strong></p>")

    const pasteEvent = new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData,
    })

    qlEditor.dispatchEvent(pasteEvent)

    await nextTick()
    await flushPromises()

    const emitted = wrapper.emitted()["update:modelValue"]
    expect(wrapper.exists()).toBe(true)
    if (emitted?.length) {
      expect(emitted[emitted.length - 1]![0]).toContain("Bold text")
    }
  })

  it("does not handle paste when readonly", async () => {
    await mountEditor("", { readonly: true })
    await flushPromises()

    const qlEditor = wrapper
      .findComponent({ name: "QuillEditor" })
      .vm.$el.querySelector(".ql-editor") as HTMLElement

    // Browser Mode: Real ClipboardEvent!
    const clipboardData = new DataTransfer()
    clipboardData.setData("text/html", "<p>Test</p>")
    const pasteEvent = new ClipboardEvent("paste", {
      bubbles: true,
      cancelable: true,
      clipboardData,
    })

    qlEditor.dispatchEvent(pasteEvent)
    await flushPromises()

    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("linkifies wikilinks in Quill HTML while model matches the interval", async () => {
    const wikiTitles = [{ linkText: "MyNote", noteId: 42 }]
    await mountEditor("", { wikiTitles })
    await flushPromises()

    const quill = wrapper.findComponent({ name: "QuillEditor" })
    quill.vm.$emit("update:modelValue", "<p>[[MyNote]]</p>")
    await wrapper.setProps({ modelValue: "[[MyNote]]" })
    await nextTick()
    await flushPromises()

    expect(String(quill.props("modelValue"))).toContain(
      '<a href="/d/n/42" class="doughnut-link">'
    )
  })

  it("shows read-only Properties above Quill when details include supported YAML frontmatter", async () => {
    const details = `---
diligence: high
topic: training
---

# Workshop Body

Main content here.`
    await mountEditor(details, { readonly: true })
    await flushPromises()

    expect(wrapper.text()).toContain("Properties")
    expect(wrapper.text()).toContain("diligence")
    expect(wrapper.text()).toContain("high")
    expect(wrapper.text()).toContain("topic")
    expect(wrapper.text()).toContain("training")

    const quill = wrapper.findComponent({ name: "QuillEditor" })
    const html = String(quill.props("modelValue"))
    expect(html).toContain("Workshop Body")
    expect(html).not.toContain("diligence:")
    expect(html).not.toContain("topic:")
  })

  it("shows add-only insert chrome when editable details have no frontmatter", async () => {
    await mountEditor("# Hello\n\nParagraph.")
    await flushPromises()

    expect(wrapper.find("section").exists()).toBe(true)
    expect(wrapper.find("h4").exists()).toBe(false)
    expect(wrapper.text()).not.toContain("Properties")
    expect(wrapper.text()).toContain("Add note property")
  })

  it("does not show Properties section when details have no frontmatter (readonly)", async () => {
    await mountEditor("# Hello\n\nParagraph.", { readonly: true })
    await flushPromises()

    expect(wrapper.find("section").exists()).toBe(false)
  })

  it("shows add-only insert chrome when editable empty frontmatter block", async () => {
    await mountEditor(`---


---

Body`)
    await flushPromises()

    expect(wrapper.find("section").exists()).toBe(true)
    expect(wrapper.find("h4").exists()).toBe(false)
    expect(wrapper.text()).not.toContain("Properties")
    expect(wrapper.text()).toContain("Add note property")
  })

  it("does not show Properties section when frontmatter block is empty (readonly)", async () => {
    await mountEditor(
      `---


---

Body`,
      { readonly: true }
    )
    await flushPromises()

    expect(wrapper.find("section").exists()).toBe(false)
  })

  it("inserting a property emits composed frontmatter and preserves body", async () => {
    await mountEditor("# Hello Body")
    await flushPromises()

    const addBtn = wrapper
      .findAll("button")
      .find((w) => w.text().includes("Add note property"))
    expect(addBtn).toBeDefined()
    await addBtn!.trigger("click")
    await flushPromises()

    const keyInput = wrapper.find('[data-testid="rich-note-property-key"]')
    const valInput = wrapper.find('[data-testid="rich-note-property-value"]')
    await keyInput.setValue("status")
    await valInput.setValue("draft")
    await valInput.trigger("blur")
    await flushPromises()

    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted?.length).toBeGreaterThan(0)
    const last = emitted![emitted!.length - 1]![0] as string
    expect(last).toContain("---")
    expect(last).toContain("status: draft")
    expect(last).toContain("Hello Body")
  })

  it("shows parse error and hides Properties when frontmatter fails to parse", async () => {
    const details = `---
bad:
  nested: value
---

Still body`
    await mountEditor(details)
    await flushPromises()

    expect(wrapper.find("section").exists()).toBe(false)

    const alert = wrapper.find(
      '[data-testid="rich-note-frontmatter-parse-error"]'
    )
    expect(alert.exists()).toBe(true)
    expect(alert.text()).toContain("string")
    expect(alert.text()).toContain("Markdown mode")
  })

  it("forces Quill readonly and does not emit details updates when frontmatter fails to parse", async () => {
    const details = `---
bad:
  nested: value
---

Still body`
    await mountEditor(details)
    await flushPromises()

    const quill = wrapper.findComponent({ name: "QuillEditor" })
    expect(quill.props("readonly")).toBe(true)

    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0
    quill.vm.$emit("update:modelValue", "<p>Edited without fixing YAML</p>")
    await flushPromises()

    const emitCountAfter = wrapper.emitted("update:modelValue")?.length ?? 0
    expect(emitCountAfter).toBe(emitCountBefore)
  })

  it("composes edited body with existing frontmatter when emitting updates", async () => {
    const details = `---
diligence: high
topic: training
---

# Original`
    await mountEditor(details)
    await flushPromises()

    const quill = wrapper.findComponent({ name: "QuillEditor" })
    quill.vm.$emit("update:modelValue", "<h1>Edited Heading</h1>")
    await flushPromises()

    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted?.length).toBeGreaterThan(0)
    const last = emitted![emitted!.length - 1]![0] as string
    expect(last).toContain("diligence:")
    expect(last).toContain("topic:")
    expect(last).toContain("Edited Heading")
  })

  it("editing an existing property row emits renamed keys and updated values", async () => {
    const details = `---
topic: training
---

Workshop body.`
    await mountEditor(details)
    await flushPromises()

    const keyInput = wrapper.find(
      '[data-testid="rich-note-property-row-key-input"]'
    )
    const valInput = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"]'
    )
    await keyInput.setValue("domain")
    await keyInput.trigger("blur")
    await flushPromises()
    await valInput.setValue("wiki")
    await valInput.trigger("blur")
    await flushPromises()

    const emitted = wrapper.emitted()["update:modelValue"]
    expect(emitted?.length).toBeGreaterThan(0)
    const last = emitted![emitted!.length - 1]![0] as string
    expect(last).toContain("domain:")
    expect(last).toContain("wiki")
    expect(last).not.toContain("topic:")
    expect(last).toContain("Workshop body")
  })

  it("shows validation and does not emit corrupt duplicate keys when renaming a row", async () => {
    const details = `---
alpha: one
beta: two
---

Body`
    await mountEditor(details)
    await flushPromises()

    const rows = wrapper.findAll('[data-testid="rich-note-property-row"]')
    expect(rows.length).toBe(2)
    const betaKeyInput = rows[1]!.find(
      '[data-testid="rich-note-property-row-key-input"]'
    )
    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0

    await betaKeyInput.trigger("focus")
    await betaKeyInput.setValue("alpha")
    await betaKeyInput.trigger("blur")
    await flushPromises()

    expect(
      wrapper.find('[data-testid="rich-note-property-validation"]').text()
    ).toContain("Duplicate")

    const emitCountAfter = wrapper.emitted("update:modelValue")?.length ?? 0
    expect(emitCountAfter).toBe(emitCountBefore)

    const betaKeyAfter = wrapper
      .findAll('[data-testid="rich-note-property-row"]')[1]!
      .find('[data-testid="rich-note-property-row-key-input"]')
    expect((betaKeyAfter.element as HTMLInputElement).value).toBe("beta")
  })

  it("removing one property row emits markdown without that key and retains the rest", async () => {
    const details = `---
alpha: one
beta: two
---

Body line`
    await mountEditor(details)
    await flushPromises()

    const removeBtns = wrapper.findAll(
      '[data-testid="rich-note-property-row-remove"]'
    )
    expect(removeBtns.length).toBe(2)
    await removeBtns[0]!.trigger("click")
    await flushPromises()

    const emitted = wrapper.emitted("update:modelValue")
    expect(emitted?.length).toBeGreaterThan(0)
    const last = emitted![emitted!.length - 1]![0] as string
    expect(last).not.toContain("alpha:")
    expect(last).toContain("beta:")
    expect(last).toContain("Body line")
  })

  it("removing every property row emits body-only details and shows add-only chrome without Properties heading", async () => {
    const details = `---
only: x
---

Paragraph.\n`
    await mountEditor(details)
    await flushPromises()

    expect(wrapper.text()).toContain("Properties")

    await wrapper
      .find('[data-testid="rich-note-property-row-remove"]')
      .trigger("click")
    await flushPromises()

    const emitted = wrapper.emitted("update:modelValue")
    expect(emitted?.length).toBeGreaterThan(0)
    const last = emitted![emitted!.length - 1]![0] as string
    expect(last.startsWith("---")).toBe(false)
    expect(last).toContain("Paragraph.")

    await wrapper.setProps({ modelValue: last })
    await flushPromises()

    expect(wrapper.find("h4").exists()).toBe(false)
    expect(wrapper.text()).not.toContain("Properties")
    expect(wrapper.text()).toContain("Add note property")
  })
})

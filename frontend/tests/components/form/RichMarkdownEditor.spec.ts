import RichMarkdownEditor from "@/components/form/RichMarkdownEditor.vue"
import { CUSTOM_RELATION_RADIO_SENTINEL } from "@/models/relationTypeOptions"
import { RICH_MODE_PRESET_PROPERTY_KEYS } from "@/utils/noteContentFrontmatter"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import { nextTick } from "vue"
import { vi } from "vitest"

describe("RichMarkdownEditor", () => {
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

  async function assertPresetOptionsVisible() {
    const options = wrapper.findAll(
      '[data-testid="rich-note-property-key-preset-option"]'
    )
    expect(options.length).toBe(RICH_MODE_PRESET_PROPERTY_KEYS.length)
    for (const key of RICH_MODE_PRESET_PROPERTY_KEYS) {
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

  it("does not emit update:modelValue on mount", async () => {
    await mountEditor("initial value")
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
    await mountEditor("# Title", { readonly: true })
    expect(wrapper.emitted()["update:modelValue"]).toBeUndefined()
  })

  it("converts pasted HTML to markdown and does not paste when readonly", async () => {
    await mountEditor("")
    await flushPromises()
    await nextTick()
    await dispatchPasteHtmlToQuill("<p><strong>Bold text</strong></p>")
    const emitted = wrapper.emitted()["update:modelValue"]
    if (emitted?.length) {
      expect(emitted[emitted.length - 1]![0]).toContain("Bold text")
    }

    await mountEditor("", { readonly: true })
    await flushPromises()
    await dispatchPasteHtmlToQuill("<p>Test</p>")
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

  it("shows read-only Properties above Quill when content includes supported YAML frontmatter", async () => {
    const markdown = `---
diligence: high
topic: training
---

# Workshop Body

Main content here.`
    await mountEditor(markdown, { readonly: true })
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

  it("shows add-only chrome when content has no or empty frontmatter, hides section when readonly", async () => {
    for (const md of [
      "# Hello\n\nParagraph.",
      `---


---

Body`,
    ]) {
      await mountEditor(md)
      expect(wrapper.find("section").exists()).toBe(true)
      expect(wrapper.find("h4").exists()).toBe(false)
      expect(wrapper.text()).not.toContain("Properties")
      expect(wrapper.text()).toContain("Add property")

      await mountEditor(md, { readonly: true })
      expect(wrapper.find("section").exists()).toBe(false)
    }
  })

  it("focuses property key when Add property is clicked", async () => {
    await mountEditor("# Hello Body")
    await flushPromises()
    await openAddProperty()
    await flushAnimationFrame()
    const keyInput = wrapper.find('[data-testid="rich-note-property-key"]')
      .element as HTMLInputElement
    expect(document.activeElement).toBe(keyInput)
  })

  it("focuses property key when Add property is clicked with existing rows", async () => {
    const markdown = `---
status: ok
---

# Body`
    await mountEditor(markdown)
    await flushPromises()
    await openAddProperty()
    await flushAnimationFrame()
    const keyInput = wrapper.find('[data-testid="rich-note-property-key"]')
      .element as HTMLInputElement
    expect(document.activeElement).toBe(keyInput)
  })

  it("inserting a property emits composed frontmatter and preserves body", async () => {
    await mountEditor("# Hello Body")
    await flushPromises()
    await openAddProperty()

    const keyInput = wrapper.find('[data-testid="rich-note-property-key"]')
    const valInput = wrapper.find('[data-testid="rich-note-property-value"]')
    await keyInput.setValue("status")
    await setWikiPropertyValueField(valInput, "draft")
    await valInput.trigger("blur")
    await flushPromises()

    const last = lastEmittedMarkdown()
    expect(last).toContain("---")
    expect(last).toContain("status: draft")
    expect(last).toContain("Hello Body")
  })

  it("shows preset keys when insert or row key input is focused", async () => {
    await mountEditor("# Hello Body")
    await flushPromises()
    await openAddProperty()
    ;(
      wrapper.find('[data-testid="rich-note-property-key"]')
        .element as HTMLInputElement
    ).focus()
    await nextTick()
    await flushPromises()
    await assertPresetOptionsVisible()

    const markdown = `---
status: ok
---

# Body`
    await mountEditor(markdown)
    await flushPromises()
    ;(
      wrapper.find('[data-testid="rich-note-property-row-key-input"]')
        .element as HTMLInputElement
    ).focus()
    await nextTick()
    await flushPromises()
    await assertPresetOptionsVisible()
  })

  it("sets property key when a preset is chosen (insert and existing row)", async () => {
    await mountEditor("# Hello Body")
    await flushPromises()
    await openAddProperty()
    ;(
      wrapper.find('[data-testid="rich-note-property-key"]')
        .element as HTMLInputElement
    ).focus()
    await nextTick()
    await flushPromises()
    await selectPresetKey("wikidata_id")
    expect(
      (
        wrapper.find('[data-testid="rich-note-property-key"]')
          .element as HTMLInputElement
      ).value
    ).toBe("wikidata_id")

    const markdown = `---
status: ok
---

# Body`
    await mountEditor(markdown)
    await flushPromises()
    ;(
      wrapper.find('[data-testid="rich-note-property-row-key-input"]')
        .element as HTMLInputElement
    ).focus()
    await nextTick()
    await flushPromises()
    await selectPresetKey("url")
    expect(
      (
        wrapper.find('[data-testid="rich-note-property-row-key-input"]')
          .element as HTMLInputElement
      ).value
    ).toBe("url")
  })

  it("invalid YAML: hides Properties, shows alert, freezes Quill, ignores body edits", async () => {
    const markdown = `---
bad:
  nested: value
---

Still body`
    await mountEditor(markdown)
    await flushPromises()

    expect(wrapper.find("section").exists()).toBe(false)
    const alert = wrapper.find(
      '[data-testid="rich-note-frontmatter-parse-error"]'
    )
    expect(alert.exists()).toBe(true)
    expect(alert.text()).toContain("string")
    expect(alert.text()).toContain("Markdown mode")

    const quill = wrapper.findComponent({ name: "QuillEditor" })
    expect(quill.props("readonly")).toBe(true)
    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0
    quill.vm.$emit("update:modelValue", "<p>Edited without fixing YAML</p>")
    await flushPromises()
    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emitCountBefore
    )
  })

  it("composes edited body with existing frontmatter when emitting updates", async () => {
    const markdown = `---
diligence: high
topic: training
---

# Original`
    await mountEditor(markdown)
    await flushPromises()

    const quill = wrapper.findComponent({ name: "QuillEditor" })
    quill.vm.$emit("update:modelValue", "<h1>Edited Heading</h1>")
    await flushPromises()

    const last = lastEmittedMarkdown()
    expect(last).toContain("diligence:")
    expect(last).toContain("topic:")
    expect(last).toContain("Edited Heading")
  })

  it("emits pasteComplete with full composed markdown so link-removal preserves frontmatter", async () => {
    const markdown = `---
topic: training
---

Hello`
    await mountEditor(markdown)
    await flushPromises()

    const quill = wrapper.findComponent({ name: "QuillEditor" })
    quill.vm.$emit(
      "pasteComplete",
      '<p>Hello <a href="https://example.com" rel="noopener noreferrer" target="_blank">x</a></p>'
    )
    await flushPromises()

    const emitted = wrapper.emitted("pasteComplete")
    expect(emitted?.length).toBeGreaterThan(0)
    const payload = emitted![emitted!.length - 1]![0] as string
    expect(payload).toContain("topic: training")
    expect(payload).toMatch(/^---\n/)
  })

  it("emits deadLinkClick when a dead wiki link in a property value is clicked", async () => {
    const markdown = `---
topic: "[[Missing Note]]"
---

Body`
    await mountEditor(markdown)
    await flushPromises()

    const valField = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"]'
    )
    const dead = valField.find("a.dead-link")
    expect(dead.exists()).toBe(true)
    await dead.trigger("click")
    await flushPromises()
    expect(wrapper.emitted("deadLinkClick")?.[0]).toEqual(["Missing Note"])
  })

  it("editing an existing property row emits renamed keys and updated values", async () => {
    const markdown = `---
topic: training
---

Workshop body.`
    await mountEditor(markdown)
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
    await setWikiPropertyValueField(valInput, "wiki")
    await valInput.trigger("blur")
    await flushPromises()

    const last = lastEmittedMarkdown()
    expect(last).toContain("domain:")
    expect(last).toContain("wiki")
    expect(last).not.toContain("topic:")
    expect(last).toContain("Workshop body")
  })

  it("shows validation and does not emit corrupt duplicate keys when renaming a row", async () => {
    const markdown = `---
alpha: one
beta: two
---

Body`
    await mountEditor(markdown)
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

    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emitCountBefore
    )

    const betaKeyAfter = wrapper
      .findAll('[data-testid="rich-note-property-row"]')[1]!
      .find('[data-testid="rich-note-property-row-key-input"]')
    expect((betaKeyAfter.element as HTMLInputElement).value).toBe("beta")
  })

  it("removing one property row emits markdown without that key and retains the rest", async () => {
    const markdown = `---
alpha: one
beta: two
---

Body line`
    await mountEditor(markdown)
    await flushPromises()

    const removeBtns = wrapper.findAll(
      '[data-testid="rich-note-property-row-remove"]'
    )
    expect(removeBtns.length).toBe(2)
    await removeBtns[0]!.trigger("click")
    await flushPromises()

    const last = lastEmittedMarkdown()
    expect(last).not.toContain("alpha:")
    expect(last).toContain("beta:")
    expect(last).toContain("Body line")
  })

  describe("relation property in rich mode", () => {
    async function mountRelation(relation: string) {
      await mountEditor(
        `---\nrelation: ${relation}\ntype: relationship\n---\n\nBody`
      )
      await flushPromises()
    }

    it("relation button shows human label for known kebab and raw text for unknown", async () => {
      await mountRelation("similar-to")
      let btn = wrapper.find('[aria-label="Relation Type"]')
      expect(btn.text()).toContain("similar to")
      expect(btn.text()).not.toContain("similar-to")

      await mountRelation("my-custom-relation")
      btn = wrapper.find('[aria-label="Relation Type"]')
      expect(btn.text()).toContain("my-custom-relation")
      expect(btn.text()).not.toContain("related to")
    })

    it("opens relation dialog for unknown relation with Custom option", async () => {
      await mountRelation("my-custom-relation")
      await wrapper.find('[aria-label="Relation Type"]').trigger("click")
      await flushPromises()
      const rt = wrapper.findComponent({ name: "RelationTypeSelect" })
      expect(rt.exists()).toBe(true)
      expect(rt.text()).toContain("Custom…")
    })

    it("commits custom relationship text from the dialog and emits updated frontmatter", async () => {
      await mountRelation("similar-to")
      await wrapper.find('[aria-label="Relation Type"]').trigger("click")
      await flushPromises()

      const rt = wrapper.findComponent({ name: "RelationTypeSelect" })
      await rt
        .find(`input[value="${CUSTOM_RELATION_RADIO_SENTINEL}"]`)
        .trigger("change")
      await flushPromises()

      const field = rt.find('input[type="text"].daisy-input-bordered')
      expect(field.exists()).toBe(true)
      await field.setValue("novel connector phrase")
      await field.trigger("keydown", { key: "Enter" })
      await flushPromises()

      expect(lastEmittedMarkdown()).toContain(
        "relation: novel-connector-phrase"
      )
    })

    it("opens dialog with custom text prefilled for an unknown relation", async () => {
      await mountRelation("xyz-unknown-kebab")
      await wrapper.find('[aria-label="Relation Type"]').trigger("click")
      await flushPromises()

      const rt = wrapper.findComponent({ name: "RelationTypeSelect" })
      const field = rt.find('input[type="text"].daisy-input-bordered')
      expect(field.exists()).toBe(true)
      expect((field.element as HTMLInputElement).value).toBe(
        "xyz-unknown-kebab"
      )
      const primaryLabelForCustom = rt.find(
        `label[for="rich-note-relation-property-${CUSTOM_RELATION_RADIO_SENTINEL}"]`
      )
      expect(primaryLabelForCustom.exists()).toBe(true)
      expect(primaryLabelForCustom.classes()).toContain("daisy-bg-primary")
    })
  })

  describe("image property upload", () => {
    let uploadSpy: ReturnType<typeof mockSdkService>

    beforeEach(() => {
      uploadSpy = mockSdkService(NoteController, "uploadNoteImage", {
        imagePath: "/attachments/images/99/e2e.png",
      })
    })

    afterEach(() => {
      vi.restoreAllMocks()
    })

    it("sets image path from upload when choosing a file on the image row", async () => {
      const markdown = `---
image: /attachments/images/1/old.png
---

# Hi`
      await mountEditor(markdown, { noteId: 42 })
      await flushPromises()

      const fileInput = wrapper.find(
        '[data-testid="rich-note-image-property-file-input"]'
      )
      const inputEl = fileInput.element as HTMLInputElement
      const file = new File(["fake"], "pic.png", { type: "image/png" })
      Object.defineProperty(inputEl, "files", {
        value: [file],
        configurable: true,
      })
      await fileInput.trigger("change")
      await flushPromises()

      expect(uploadSpy).toHaveBeenCalled()
      const last = lastEmittedMarkdown()
      expect(last).toContain("image: /attachments/images/99/e2e.png")
      expect(last).toContain("# Hi")
    })
  })

  it("removing every property row emits body-only markdown and shows add-only chrome without Properties heading", async () => {
    const markdown = `---
only: x
---

Paragraph.\n`
    await mountEditor(markdown)
    await flushPromises()

    expect(wrapper.text()).toContain("Properties")

    await wrapper
      .find('[data-testid="rich-note-property-row-remove"]')
      .trigger("click")
    await flushPromises()

    const last = lastEmittedMarkdown()
    expect(last.startsWith("---")).toBe(false)
    expect(last).toContain("Paragraph.")

    await wrapper.setProps({ modelValue: last })
    await flushPromises()

    expect(wrapper.find("h4").exists()).toBe(false)
    expect(wrapper.text()).not.toContain("Properties")
    expect(wrapper.text()).toContain("Add property")
  })
})

import { CUSTOM_RELATION_RADIO_SENTINEL } from "@/models/relationTypeOptions"
import {
  INDEX_ONLY_PRESET_PROPERTY_KEYS,
  richModeKeyDropdownPresetKeysForPropertyRows,
} from "@/utils/noteContentFrontmatter"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  mountSoftKeyboardPrimer,
  softKeyboardPrimerElement,
  waitUntilFocused,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import { nextTick } from "vue"
import { vi } from "vitest"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const addPropertyTapCases = [
  {
    case: "no existing rows",
    markdown: "# Hello Body",
  },
  {
    case: "existing rows",
    markdown: `---
status: ok
---

# Body`,
  },
] as const

const existingPropertyValueMarkdown = `---
topic: training
---

Workshop body.`

describe("RichMarkdownEditor properties", () => {
  const h = createRichMarkdownEditorTestHarness()
  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    h.cleanup()
  })

  it("shows read-only Properties above Quill when content includes supported YAML frontmatter", async () => {
    const markdown = `---
diligence: high
topic: training
---

# Workshop Body

Main content here.`
    const wrapper = await h.mountEditor(markdown, { readonly: true })
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
      let wrapper = await h.mountEditor(md)
      expect(wrapper.find("section").exists()).toBe(true)
      expect(wrapper.find("h4").exists()).toBe(false)
      expect(wrapper.text()).not.toContain("Properties")
      expect(wrapper.text()).toContain("Add property")

      wrapper = await h.mountEditor(md, { readonly: true })
      expect(wrapper.find("section").exists()).toBe(false)
    }
  })

  describe("soft keyboard primer", () => {
    it.each(
      addPropertyTapCases
    )("focuses primer synchronously when Add property is tapped with $case on touch device", async ({
      markdown,
    }) => {
      matchMediaSpy = mockCoarsePointer(true)
      mountSoftKeyboardPrimer()
      await h.mountEditor(markdown)
      await flushPromises()
      const primer = softKeyboardPrimerElement()
      expect(primer).toBeTruthy()

      h.tapAddProperty()

      expect(document.activeElement).toBe(primer)
    })

    it.each(
      addPropertyTapCases
    )("transfers focus to property key after insert form mounts with $case", async ({
      markdown,
    }) => {
      matchMediaSpy = mockCoarsePointer(true)
      mountSoftKeyboardPrimer()
      await h.mountEditor(markdown)
      await flushPromises()

      await h.openAddProperty()
      await h.flushAnimationFrame()

      await waitUntilFocused('[data-testid="rich-note-property-key"]')
    })

    it("does not focus primer when pointer is not coarse", async () => {
      matchMediaSpy = mockCoarsePointer(false)
      mountSoftKeyboardPrimer()
      await h.mountEditor("# Hello Body")
      await flushPromises()
      const primer = softKeyboardPrimerElement()

      await h.openAddProperty()
      await h.flushAnimationFrame()

      expect(document.activeElement).not.toBe(primer)
      await waitUntilFocused('[data-testid="rich-note-property-key"]')
    })

    describe("existing property value", () => {
      async function mountForPropertyValuePrimer(coarse: boolean) {
        matchMediaSpy = mockCoarsePointer(coarse)
        mountSoftKeyboardPrimer()
        await h.mountEditor(existingPropertyValueMarkdown)
        await flushPromises()
        return softKeyboardPrimerElement()
      }

      it("focuses primer synchronously when value is pointerdown-tapped on touch device", async () => {
        const primer = await mountForPropertyValuePrimer(true)
        expect(primer).toBeTruthy()

        h.pointerdownPropertyValueField()

        expect(document.activeElement).toBe(primer)
      })

      it("transfers focus to value field after pointerdown on touch device", async () => {
        await mountForPropertyValuePrimer(true)

        h.pointerdownPropertyValueField()
        h.completePropertyValueFieldTap()

        await waitUntilFocused(
          '[data-testid="rich-note-property-row-value-input"]'
        )
      })

      it("does not focus primer when pointer is not coarse", async () => {
        const primer = await mountForPropertyValuePrimer(false)

        h.pointerdownPropertyValueField()
        h.completePropertyValueFieldTap()

        expect(document.activeElement).not.toBe(primer)
        await waitUntilFocused(
          '[data-testid="rich-note-property-row-value-input"]'
        )
      })

      it("does not focus primer when pointerdown hits a dead wiki link", async () => {
        matchMediaSpy = mockCoarsePointer(true)
        mountSoftKeyboardPrimer()
        await h.mountEditor(`---
topic: "[[Missing Note]]"
---

Body`)
        await flushPromises()
        const primer = softKeyboardPrimerElement()
        const deadLink = h
          .propertyValueFieldElement()
          .querySelector("a.dead-link")
        expect(deadLink).toBeTruthy()

        deadLink!.dispatchEvent(
          new PointerEvent("pointerdown", { bubbles: true })
        )

        expect(document.activeElement).not.toBe(primer)
      })
    })
  })

  it("inserting a property emits composed frontmatter and preserves body", async () => {
    await h.mountEditor("# Hello Body")
    await flushPromises()
    await h.openAddProperty()

    const keyInput = h
      .getWrapper()
      .find('[data-testid="rich-note-property-key"]')
    const valInput = h
      .getWrapper()
      .find('[data-testid="rich-note-property-value"]')
    await keyInput.setValue("status")
    await h.setWikiPropertyValueField(valInput, "draft")
    await valInput.trigger("blur")
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("---")
    expect(last).toContain("status: draft")
    expect(last).toContain("Hello Body")
  })

  it("shows preset keys when insert or row key input is focused", async () => {
    await h.mountEditor("# Hello Body")
    await flushPromises()
    await h.openAddProperty()
    ;(
      h.getWrapper().find('[data-testid="rich-note-property-key"]')
        .element as HTMLInputElement
    ).focus()
    await nextTick()
    await flushPromises()
    await h.assertPresetOptionsVisible(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [])
    )

    const markdown = `---
status: ok
---

# Body`
    await h.mountEditor(markdown)
    await flushPromises()
    ;(
      h.getWrapper().find('[data-testid="rich-note-property-row-key-input"]')
        .element as HTMLInputElement
    ).focus()
    await nextTick()
    await flushPromises()
    await h.assertPresetOptionsVisible(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        { key: "status", value: "ok" },
      ])
    )
  })

  it("sets property key when a preset is chosen (insert and existing row)", async () => {
    await h.mountEditor("# Hello Body")
    await flushPromises()
    await h.openAddProperty()
    ;(
      h.getWrapper().find('[data-testid="rich-note-property-key"]')
        .element as HTMLInputElement
    ).focus()
    await nextTick()
    await flushPromises()
    await h.selectPresetKey("wikidata_id")
    expect(
      (
        h.getWrapper().find('[data-testid="rich-note-property-key"]')
          .element as HTMLInputElement
      ).value
    ).toBe("wikidata_id")

    const markdown = `---
status: ok
---

# Body`
    await h.mountEditor(markdown)
    await flushPromises()
    ;(
      h.getWrapper().find('[data-testid="rich-note-property-row-key-input"]')
        .element as HTMLInputElement
    ).focus()
    await nextTick()
    await flushPromises()
    await h.selectPresetKey("url")
    expect(
      (
        h.getWrapper().find('[data-testid="rich-note-property-row-key-input"]')
          .element as HTMLInputElement
      ).value
    ).toBe("url")
  })

  it("property key preset dropdown excludes keys already in frontmatter", async () => {
    const markdown = `---
image: /x.png
---

# Body`
    await h.mountEditor(markdown)
    await flushPromises()
    await h.openAddProperty()
    ;(
      h.getWrapper().find('[data-testid="rich-note-property-key"]')
        .element as HTMLInputElement
    ).focus()
    await nextTick()
    await flushPromises()
    await h.assertPresetOptionsVisible(
      richModeKeyDropdownPresetKeysForPropertyRows(false, [
        { key: "image", value: "/x.png" },
      ])
    )
  })

  it("invalid YAML: hides Properties, shows alert, freezes Quill, ignores body edits", async () => {
    const markdown = `---
bad:
  nested: value
---

Still body`
    const wrapper = await h.mountEditor(markdown)
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
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    const quill = wrapper.findComponent({ name: "QuillEditor" })
    quill.vm.$emit("update:modelValue", "<h1>Edited Heading</h1>")
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("diligence:")
    expect(last).toContain("topic:")
    expect(last).toContain("Edited Heading")
  })

  it("emits pasteComplete with full composed markdown so link-removal preserves frontmatter", async () => {
    const markdown = `---
topic: training
---

Hello`
    const wrapper = await h.mountEditor(markdown)
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
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    const valField = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"]'
    )
    const dead = valField.find("a.dead-link")
    expect(dead.exists()).toBe(true)
    await dead.trigger("click")
    await flushPromises()
    expect(wrapper.emitted("deadLinkClick")?.[0]).toEqual([
      { targetToken: "Missing Note", displayText: "Missing Note" },
    ])
  })

  it("emits deadLinkClick with target token when a property wiki link uses display text", async () => {
    const markdown = `---
topic: "[[Ghost Page|shown text]]"
---

Body`
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    const valField = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"]'
    )
    const dead = valField.find("a.dead-link")
    expect(dead.exists()).toBe(true)
    await dead.trigger("click")
    await flushPromises()
    expect(wrapper.emitted("deadLinkClick")?.[0]).toEqual([
      { targetToken: "Ghost Page", displayText: "shown text" },
    ])
  })

  it("editing an existing property row emits renamed keys and updated values", async () => {
    const markdown = `---
topic: training
---

Workshop body.`
    const wrapper = await h.mountEditor(markdown)
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
    await h.setWikiPropertyValueField(valInput, "wiki")
    await valInput.trigger("blur")
    await flushPromises()

    const last = h.lastEmittedMarkdown()
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
    const wrapper = await h.mountEditor(markdown)
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
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    const removeBtns = wrapper.findAll(
      '[data-testid="rich-note-property-row-remove"]'
    )
    expect(removeBtns.length).toBe(2)
    await removeBtns[0]!.trigger("click")
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).not.toContain("alpha:")
    expect(last).toContain("beta:")
    expect(last).toContain("Body line")
  })

  describe("relation property in rich mode", () => {
    async function mountRelation(relation: string) {
      await h.mountEditor(
        `---\nrelation: ${relation}\ntype: relationship\n---\n\nBody`
      )
      await flushPromises()
    }

    it("relation button shows human label for known kebab and raw text for unknown", async () => {
      await mountRelation("similar-to")
      let btn = h.getWrapper().find('[aria-label="Relation Type"]')
      expect(btn.text()).toContain("similar to")
      expect(btn.text()).not.toContain("similar-to")

      await mountRelation("my-custom-relation")
      btn = h.getWrapper().find('[aria-label="Relation Type"]')
      expect(btn.text()).toContain("my-custom-relation")
      expect(btn.text()).not.toContain("related to")
    })

    it("opens relation dialog for unknown relation with Custom option", async () => {
      await mountRelation("my-custom-relation")
      await h.getWrapper().find('[aria-label="Relation Type"]').trigger("click")
      await flushPromises()
      const rt = h.getWrapper().findComponent({ name: "RelationTypeSelect" })
      expect(rt.exists()).toBe(true)
      expect(rt.text()).toContain("Custom…")
    })

    it("commits custom relationship text from the dialog and emits updated frontmatter", async () => {
      await mountRelation("similar-to")
      await h.getWrapper().find('[aria-label="Relation Type"]').trigger("click")
      await flushPromises()

      const rt = h.getWrapper().findComponent({ name: "RelationTypeSelect" })
      await rt
        .find(`input[value="${CUSTOM_RELATION_RADIO_SENTINEL}"]`)
        .trigger("change")
      await flushPromises()

      const field = rt.find('input[type="text"].daisy-input')
      expect(field.exists()).toBe(true)
      await field.setValue("novel connector phrase")
      await field.trigger("keydown", { key: "Enter" })
      await flushPromises()

      expect(h.lastEmittedMarkdown()).toContain(
        "relation: novel-connector-phrase"
      )
    })

    it("opens dialog with custom text prefilled for an unknown relation", async () => {
      await mountRelation("xyz-unknown-kebab")
      await h.getWrapper().find('[aria-label="Relation Type"]').trigger("click")
      await flushPromises()

      const rt = h.getWrapper().findComponent({ name: "RelationTypeSelect" })
      const field = rt.find('input[type="text"].daisy-input')
      expect(field.exists()).toBe(true)
      expect((field.element as HTMLInputElement).value).toBe(
        "xyz-unknown-kebab"
      )
      const primaryLabelForCustom = rt.find(
        `label[for="rich-note-relation-property-${CUSTOM_RELATION_RADIO_SENTINEL}"]`
      )
      expect(primaryLabelForCustom.exists()).toBe(true)
      expect(primaryLabelForCustom.classes()).toContain("bg-primary")
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
      const wrapper = await h.mountEditor(markdown, { noteId: 42 })
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
      const last = h.lastEmittedMarkdown()
      expect(last).toContain("image: /attachments/images/99/e2e.png")
      expect(last).toContain("# Hi")
    })
  })

  it("removing every property row emits body-only markdown and shows add-only chrome without Properties heading", async () => {
    const markdown = `---
only: x
---

Paragraph.\n`
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    expect(wrapper.text()).toContain("Properties")

    await wrapper
      .find('[data-testid="rich-note-property-row-remove"]')
      .trigger("click")
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last.startsWith("---")).toBe(false)
    expect(last).toContain("Paragraph.")

    await wrapper.setProps({ modelValue: last })
    await flushPromises()

    expect(wrapper.find("h4").exists()).toBe(false)
    expect(wrapper.text()).not.toContain("Properties")
    expect(wrapper.text()).toContain("Add property")
  })

  describe("index-only predefined properties", () => {
    it("does not show index-only predefined rows when isIndexContext is false", async () => {
      const wrapper = await h.mountEditor("# Body")
      await flushPromises()

      const keyInputs = wrapper.findAll(
        '[data-testid="rich-note-property-row-key-input"]'
      )
      const keyValues = keyInputs.map(
        (el) => (el.element as HTMLInputElement).value
      )
      for (const key of INDEX_ONLY_PRESET_PROPERTY_KEYS) {
        expect(keyValues).not.toContain(key)
      }
    })

    it("empty index-only fields are not included in emitted YAML", async () => {
      const wrapper = await h.mountEditor("# Body", { isIndexContext: true })
      await flushPromises()

      const quill = wrapper.findComponent({ name: "QuillEditor" })
      quill.vm.$emit("update:modelValue", "<h1>Updated Body</h1>")
      await flushPromises()

      const last = h.lastEmittedMarkdown()
      expect(last).not.toContain("title_pattern")
      expect(last).not.toContain("question_generation_instruction")
      expect(last).toContain("Updated Body")
    })

    it("index-only fields are shown when note already has those keys in frontmatter", async () => {
      const markdown = `---
title_pattern: "{{date}}"
question_generation_instruction: Focus on facts.
---

# Body`
      const wrapper = await h.mountEditor(markdown, { isIndexContext: true })
      await flushPromises()

      const keyInputs = wrapper.findAll(
        '[data-testid="rich-note-property-row-key-input"]'
      )
      const keyValues = keyInputs.map(
        (el) => (el.element as HTMLInputElement).value
      )
      expect(keyValues).toContain("title_pattern")
      expect(keyValues).toContain("question_generation_instruction")
      expect(wrapper.text()).toContain("{{date}}")
      expect(wrapper.text()).toContain("Focus on facts.")
    })
  })
})

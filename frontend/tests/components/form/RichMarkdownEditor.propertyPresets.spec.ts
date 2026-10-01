import { advanceAnimationFrame } from "@tests/helpers/focusTargetTestSupport"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { nextTick } from "vue"
import {
  listPropertyValue,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import { propertyRowWithScalar } from "@/utils/noteContentPropertyRows"
import {
  expectPresetOptions,
  selectPresetKey,
  propertyInputEl,
  expectPropertyInputFocused,
} from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const INSERT_KEY_INPUT = '[data-testid="rich-note-property-key"]'
const ROW_KEY_INPUT = '[data-testid="rich-note-property-row-key-input"]'
const ROW_VALUE_INPUT = '[data-testid="rich-note-property-row-value-input"]'

describe("RichMarkdownEditor property presets", () => {
  const h = createRichMarkdownEditorTestHarness()

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["requestAnimationFrame"] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
    vi.useRealTimers()
  })

  describe("key presets", () => {
    it.each([
      {
        name: "promotes an occupied scalar to an ordered list",
        frontmatter: 'example of: "[[run]]"',
        key: "example of",
        added: "[[past tense]]",
        properties: {
          "example of": listPropertyValue(["[[run]]", "[[past tense]]"]),
        },
      },
      {
        name: "appends to an occupied list in order",
        frontmatter: 'url: ["https://one.example", "https://two.example"]',
        key: "url",
        added: "https://three.example",
        properties: {
          url: listPropertyValue([
            "https://one.example",
            "https://two.example",
            "https://three.example",
          ]),
        },
      },
      {
        name: "keeps a legacy numbered row separate from the base",
        frontmatter: 'example of 2: "[[legacy]]"',
        key: "example of",
        added: "[[run]]",
        properties: {
          "example of 2": propertyRowWithScalar("example of 2", "[[legacy]]")
            .value,
          "example of": propertyRowWithScalar("example of", "[[run]]").value,
        },
      },
    ])(
      "preset selection $name",
      async ({ frontmatter, key, added, properties }) => {
        await h.mountEditor(`---\n${frontmatter}\n---\n\n# Body`, {
          attachToBody: true,
        })
        await h.openAddProperty()
        await advanceAnimationFrame()
        expectPresetOptions([
          "aliases",
          "overlaps",
          "note_level",
          "image",
          "wikidata_id",
          "url",
          "example of",
          "question_generation_instruction",
        ])
        await selectPresetKey(key)
        const value = h
          .getWrapper()
          .find('[data-testid="rich-note-property-value"]')
        await h.setPropertyValueField(value, added)
        await value.trigger("blur")
        await flushPromises()
        const parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
        expect(parsed.ok).toBe(true)
        if (!parsed.ok) return
        expect(parsed.properties).toEqual(properties)
      }
    )

    it("offers available presets and sets keys for existing and inserted rows", async () => {
      await h.mountEditor(
        `---
custom: workshop
image: /x.png
---

# Body`,
        { attachToBody: true }
      )
      const existingKeyInput = propertyInputEl(ROW_KEY_INPUT)
      existingKeyInput.focus()
      await nextTick()
      await flushPromises()
      expectPresetOptions([
        "aliases",
        "overlaps",
        "note_level",
        "wikidata_id",
        "url",
        "example of",
        "question_generation_instruction",
      ])

      await selectPresetKey("url")
      expect(existingKeyInput.value).toBe("url")
      expectPropertyInputFocused(`[data-property-key="url"] ${ROW_VALUE_INPUT}`)

      await h.openAddProperty()
      await advanceAnimationFrame()
      expectPresetOptions([
        "aliases",
        "overlaps",
        "note_level",
        "wikidata_id",
        "url",
        "example of",
        "question_generation_instruction",
      ])
      await selectPresetKey("wikidata_id")
      expect(propertyInputEl(INSERT_KEY_INPUT).value).toBe("wikidata_id")
      expectPropertyInputFocused(
        '[data-testid="rich-note-wikidata-property-insert-edit"]'
      )
    })
  })

  describe("key preset narrowing", () => {
    async function typeKey(selector: string, text: string) {
      const el = propertyInputEl(selector)
      el.value = text
      el.dispatchEvent(new Event("input", { bubbles: true }))
      await flushPromises()
    }

    async function openAddFormKey() {
      await h.mountEditor("# Body", { attachToBody: true })
      await h.openAddProperty()
      await advanceAnimationFrame()
    }

    it.each([
      { typed: "ur", listed: ["url"] },
      { typed: "UR", listed: ["url"] },
      { typed: "of", listed: ["example of"] },
      { typed: "mo", listed: [] },
    ])("add form key typed $typed lists $listed", async ({ typed, listed }) => {
      await openAddFormKey()
      await typeKey(INSERT_KEY_INPUT, typed)
      expectPresetOptions(listed)
    })

    it("add form lists every available preset once the typed text is cleared", async () => {
      await openAddFormKey()
      const all = [
        "aliases",
        "overlaps",
        "note_level",
        "image",
        "wikidata_id",
        "url",
        "example of",
        "question_generation_instruction",
      ]
      await typeKey(INSERT_KEY_INPUT, "ur")
      await typeKey(INSERT_KEY_INPUT, "")
      expectPresetOptions(all)
    })

    it("existing row lists every available preset on focus and narrows on typing", async () => {
      await h.mountEditor("---\ncustom: workshop\n---\n\n# Body", {
        attachToBody: true,
      })
      propertyInputEl(ROW_KEY_INPUT).focus()
      await flushPromises()
      expectPresetOptions([
        "aliases",
        "overlaps",
        "note_level",
        "image",
        "wikidata_id",
        "url",
        "example of",
        "question_generation_instruction",
      ])

      await typeKey(ROW_KEY_INPUT, "ur")
      expectPresetOptions(["url"])
    })

    it("choosing a narrowed preset closes the list and focuses the row value", async () => {
      await h.mountEditor("---\ncustom: workshop\n---\n\n# Body", {
        attachToBody: true,
      })
      propertyInputEl(ROW_KEY_INPUT).focus()
      await flushPromises()
      await typeKey(ROW_KEY_INPUT, "ur")
      await selectPresetKey("url")
      expectPresetOptions([])
      expectPropertyInputFocused(`[data-property-key="url"] ${ROW_VALUE_INPUT}`)
    })
  })
})

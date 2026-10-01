import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { advanceAnimationFrame } from "@tests/helpers/focusTargetTestSupport"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  mountSoftKeyboardPrimer,
  softKeyboardPrimerElement,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { noteShowLocation } from "@/routes/noteShowLocation"
import {
  listPropertyValue,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import { propertyRowWithScalar } from "@/utils/noteContentPropertyRows"
import {
  attemptRenamePropertyKey,
  expectPropertyInputFocused,
} from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const INSERT_KEY_INPUT = '[data-testid="rich-note-property-key"]'
const ROW_VALUE_INPUT = '[data-testid="rich-note-property-row-value-input"]'

describe("RichMarkdownEditor property entry", () => {
  const h = createRichMarkdownEditorTestHarness()

  async function mountTouchFocusEditor(markdown: string, coarse: boolean) {
    mockCoarsePointer(coarse)
    mountSoftKeyboardPrimer()
    await h.mountEditor(markdown, { attachToBody: true })
    return softKeyboardPrimerElement()
  }

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["requestAnimationFrame"] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
    vi.useRealTimers()
  })

  describe("inserting a property", () => {
    it("emits composed frontmatter and preserves body", async () => {
      await h.mountEditor("# Hello Body")
      await h.openAddProperty()
      await advanceAnimationFrame()

      const keyInput = h.getWrapper().find(INSERT_KEY_INPUT)
      const valInput = h
        .getWrapper()
        .find('[data-testid="rich-note-property-value"]')
      await keyInput.setValue("status")
      await h.setPropertyValueField(valInput, "draft")
      await valInput.trigger("blur")

      const last = h.lastEmittedMarkdown()
      expect(last).toContain("---")
      expect(last).toContain("status: draft")
      expect(last).toContain("Hello Body")
    })

    it("appends to exact list-capable keys without folding legacy suffixes", async () => {
      await h.mountEditor(
        `---
example of: "[[A]]"
example of 2: "[[B]]"
---

# Body`,
        { attachToBody: true }
      )

      await h.commitInsertProperty("example of", "[[C]]")
      const parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
      expect(parsed.ok).toBe(true)
      if (!parsed.ok) return
      expect(parsed.properties["example of"]).toEqual(
        listPropertyValue(["[[A]]", "[[C]]"])
      )
      expect(parsed.properties["example of 2"]).toEqual(
        propertyRowWithScalar("example of 2", "[[B]]").value
      )
    })
  })

  describe("touch focus", () => {
    it.each([
      { case: "no existing rows", markdown: "# Hello Body" },
      { case: "existing rows", markdown: "---\nstatus: ok\n---\n\n# Body" },
    ])(
      "Add property on touch focuses primer then property key with $case",
      async ({ markdown }) => {
        const primer = await mountTouchFocusEditor(markdown, true)
        expect(primer).toBeTruthy()

        h.tapAddProperty()
        expect(document.activeElement).toBe(primer)

        await flushPromises()
        await advanceAnimationFrame()
        expectPropertyInputFocused(INSERT_KEY_INPUT)
      }
    )

    it("does not focus primer on Add property when pointer is not coarse", async () => {
      const primer = await mountTouchFocusEditor("# Hello Body", false)

      await h.openAddProperty()
      await advanceAnimationFrame()
      expect(document.activeElement).not.toBe(primer)
      expectPropertyInputFocused(INSERT_KEY_INPUT)
    })

    it("focuses primer then existing value field on touch; skips primer for dead wiki link", async () => {
      const primer = await mountTouchFocusEditor(
        `---
plain: training
wiki: "[[Missing Note]]"
---

Workshop body.`,
        true
      )
      expect(primer).toBeTruthy()

      h.pointerdownPropertyValueField()
      expect(document.activeElement).toBe(primer)

      h.completePropertyValueFieldTap()
      expectPropertyInputFocused(ROW_VALUE_INPUT)

      const deadLink = h
        .getWrapper()
        .element.querySelector(`${ROW_VALUE_INPUT} a.dead-wiki-link`)
      expect(deadLink).toBeTruthy()
      deadLink!.dispatchEvent(
        new PointerEvent("pointerdown", { bubbles: true })
      )
      expect(document.activeElement).not.toBe(primer)
    })

    it("does not focus primer on an existing value field when pointer is not coarse", async () => {
      const primer = await mountTouchFocusEditor(
        "---\ntopic: training\n---\n\nWorkshop body.",
        false
      )

      h.pointerdownPropertyValueField()
      h.completePropertyValueFieldTap()

      expect(document.activeElement).not.toBe(primer)
      expectPropertyInputFocused(ROW_VALUE_INPUT)
    })
  })

  it("retains a rename and newer value while its guard waits across a body refresh", async () => {
    const response = wrapSdkResponse({ memoryTrackers: [] })
    let resolveNoteInfo!: (value: typeof response) => void
    const noteInfo = new Promise<typeof response>((resolve) => {
      resolveNoteInfo = resolve
    })
    const getNoteInfo = mockSdkService(NoteController, "getNoteInfo", {
      memoryTrackers: [],
    }).mockReturnValue(noteInfo)
    const wrapper = await h.mountEditor("---\ntopic: wiki\n---\n\nBody.", {
      noteId: 42,
      route: noteShowLocation(42),
    })
    await attemptRenamePropertyKey(wrapper, 0, "domain")
    expect(getNoteInfo).toHaveBeenCalled()
    await h.setPropertyValueField(wrapper.find(ROW_VALUE_INPUT), "newer value")
    await wrapper.setProps({
      modelValue: '---\ntopic: "wiki"\n---\n\nRefreshed body.',
    })
    expect(h.quillEditorEl().textContent).toContain("Refreshed body.")
    resolveNoteInfo(response)
    await flushPromises()
    expect(h.lastEmittedMarkdown()).toBe(
      "---\ndomain: newer value\n---\n\nRefreshed body."
    )
  })
})

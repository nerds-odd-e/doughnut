import { flushPromises } from "@vue/test-utils"
import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  mountSoftKeyboardPrimer,
  softKeyboardPrimerElement,
  waitUntilFocused,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
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

describe("RichMarkdownEditor property touch focus", () => {
  const h = createRichMarkdownEditorTestHarness()
  let matchMediaSpy: ReturnType<typeof mockCoarsePointer> | undefined

  afterEach(() => {
    matchMediaSpy?.mockRestore()
    h.cleanup()
  })

  it.each(
    addPropertyTapCases
  )("focuses primer synchronously when Add property is tapped with $case on touch device", async ({
    markdown,
  }) => {
    matchMediaSpy = mockCoarsePointer(true)
    mountSoftKeyboardPrimer()
    await h.mountEditor(markdown, { attachToBody: true })
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
    await h.mountEditor(markdown, { attachToBody: true })
    await flushPromises()

    await h.openAddProperty()
    await h.flushAnimationFrame()
    const keyInput = document.querySelector(
      '[data-testid="rich-note-property-key"]'
    )
    expect(document.activeElement).toBe(keyInput)
  })

  it("does not focus primer when pointer is not coarse", async () => {
    matchMediaSpy = mockCoarsePointer(false)
    mountSoftKeyboardPrimer()
    await h.mountEditor("# Hello Body", { attachToBody: true })
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
      await h.mountEditor(existingPropertyValueMarkdown, {
        attachToBody: true,
      })
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
      await h.mountEditor(
        `---
topic: "[[Missing Note]]"
---

Body`,
        { attachToBody: true }
      )
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

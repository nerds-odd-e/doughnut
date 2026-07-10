import { mockCoarsePointer } from "@tests/helpers/mockCoarsePointer"
import {
  mountSoftKeyboardPrimer,
  softKeyboardPrimerElement,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import { expect } from "vitest"
import type { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

type Harness = ReturnType<typeof createRichMarkdownEditorTestHarness>

export const PROPERTY_KEY_INPUT = '[data-testid="rich-note-property-key"]'
export const PROPERTY_VALUE_INPUT =
  '[data-testid="rich-note-property-row-value-input"]'

export const addPropertyTapCases = [
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

export const existingPropertyValueMarkdown = `---
topic: training
---

Workshop body.`

export const deadLinkPropertyMarkdown = `---
topic: "[[Missing Note]]"
---

Body`

export function expectElementFocused(selector: string) {
  const element = document.querySelector(selector) as HTMLElement | null
  expect(element).toBeTruthy()
  expect(document.activeElement).toBe(element)
}

export async function mountTouchFocusEditor(
  h: Harness,
  markdown: string,
  coarse: boolean
) {
  const matchMediaSpy = mockCoarsePointer(coarse)
  mountSoftKeyboardPrimer()
  await h.mountEditor(markdown, { attachToBody: true })
  return {
    matchMediaSpy,
    primer: softKeyboardPrimerElement(),
  }
}

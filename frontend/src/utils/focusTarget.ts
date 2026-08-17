import { nextTick } from "vue"

const preferredFocusableSelector = [
  "input:not([type='hidden']):not([type='file']):not([disabled])",
  "textarea:not([disabled])",
  "select:not([disabled])",
  "[contenteditable='true']",
].join(", ")

const fallbackFocusableSelector = [
  "button:not([disabled])",
  "a[href]",
  "[tabindex]:not([tabindex='-1'])",
].join(", ")

export const autofocusSelector = "[data-autofocus], [autofocus]"

export const softKeyboardPrimerId = "soft-keyboard-primer"

function touchPrimaryInputDevice(): boolean {
  return window.matchMedia("(pointer: coarse)").matches
}

export function primeSoftKeyboard(): void {
  if (!touchPrimaryInputDevice()) return
  document.getElementById(softKeyboardPrimerId)?.focus()
}

interface FocusTargetOptions {
  selectAll?: boolean
}

function isHTMLElement(element: Element | null): element is HTMLElement {
  return element instanceof HTMLElement
}

function focusableWithin(
  element: Element,
  selector: string
): HTMLElement | null {
  if (element.matches(selector) && isHTMLElement(element)) {
    return element
  }
  const focusable = element.querySelector(selector)
  return isHTMLElement(focusable) ? focusable : null
}

function selectAllText(element: HTMLElement) {
  if (
    element instanceof HTMLInputElement ||
    element instanceof HTMLTextAreaElement
  ) {
    element.select()
    return
  }
  if (!element.isContentEditable) return
  const range = document.createRange()
  range.selectNodeContents(element)
  const sel = window.getSelection()
  sel?.removeAllRanges()
  sel?.addRange(range)
}

function collapseCaretToEnd(element: HTMLElement) {
  if (
    element instanceof HTMLInputElement ||
    element instanceof HTMLTextAreaElement
  ) {
    const len = element.value.length
    element.setSelectionRange(len, len)
    return
  }
  if (!element.isContentEditable) return
  const textNode = element.firstChild
  const range = document.createRange()
  if (textNode?.nodeType === Node.TEXT_NODE) {
    const len = (textNode as Text).length
    range.setStart(textNode, len)
    range.setEnd(textNode, len)
  } else {
    range.selectNodeContents(element)
    range.collapse(false)
  }
  const sel = window.getSelection()
  sel?.removeAllRanges()
  sel?.addRange(range)
}

export function focusTargetWithin(
  element: Element | null,
  options: FocusTargetOptions = {}
): boolean {
  if (!element) return false

  const focusable =
    focusableWithin(element, preferredFocusableSelector) ??
    focusableWithin(element, fallbackFocusableSelector)

  if (!focusable) return false
  focusable.focus()
  if (options.selectAll) {
    selectAllText(focusable)
  } else {
    collapseCaretToEnd(focusable)
  }
  return true
}

function scheduleFocus(run: () => void) {
  nextTick(() => {
    requestAnimationFrame(run)
  })
}

export function scheduleFocusTargetWithin(
  element: Element | null,
  options: FocusTargetOptions = {}
) {
  scheduleFocus(() => {
    focusTargetWithin(element, options)
  })
}

export function focusAutofocusTargetWithin(element: Element | null): boolean {
  if (!element) return false
  const autofocusTarget = element.querySelector(autofocusSelector)
  return focusTargetWithin(autofocusTarget, {
    selectAll:
      autofocusTarget instanceof HTMLElement &&
      autofocusTarget.dataset.autofocusSelectAll === "true",
  })
}

export function scheduleFocusAutofocusTargetWithin(element: Element | null) {
  scheduleFocus(() => {
    focusAutofocusTargetWithin(element)
  })
}

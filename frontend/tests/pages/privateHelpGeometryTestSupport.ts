import { flushPromises } from "@vue/test-utils"
import { expect } from "vitest"

export async function settleScrolling(dialog: HTMLDialogElement) {
  await flushPromises()
  let previous = ""
  let stableFrames = 0
  const deadline = performance.now() + 3000
  while (stableFrames < 4 && performance.now() < deadline) {
    await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
    const geometry = [dialog, ...dialog.querySelectorAll<HTMLElement>("div")]
      .map(
        (element) => `${element.scrollTop},${element.getBoundingClientRect().y}`
      )
      .join(";")
    stableFrames = geometry === previous ? stableFrames + 1 : 0
    previous = geometry
  }
  expect(stableFrames).toBe(4)
}

export function expectPrivateHelpControlsInView(dialog: HTMLDialogElement) {
  const heading = dialog.querySelector<HTMLElement>("h2")!
  const close = dialog.querySelector<HTMLButtonElement>(
    '[aria-label="Close modal"]'
  )!
  for (const element of [heading, close]) {
    const bounds = element.getBoundingClientRect()
    expect(bounds.width).toBeGreaterThan(0)
    expect(bounds.height).toBeGreaterThan(0)
    expect(bounds.top).toBeGreaterThanOrEqual(0)
    expect(bounds.bottom).toBeLessThanOrEqual(window.innerHeight)
    expect(bounds.left).toBeGreaterThanOrEqual(0)
    expect(bounds.right).toBeLessThanOrEqual(window.innerWidth)
    expect(
      element.contains(
        document.elementFromPoint(
          bounds.x + bounds.width / 2,
          bounds.y + bounds.height / 2
        )
      )
    ).toBe(true)
  }
  return close
}

export async function readContentEndsInView(
  dialog: HTMLDialogElement,
  element: HTMLElement
) {
  const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT)
  const nodes: Text[] = []
  let current = walker.nextNode()
  while (current) {
    if (current.textContent?.trim()) nodes.push(current as Text)
    current = walker.nextNode()
  }
  expect(nodes.length).toBeGreaterThan(0)
  for (const end of ["first", "last"] as const) {
    const node = end === "first" ? nodes[0]! : nodes.at(-1)!
    const offset =
      end === "first" ? node.data.search(/\S/) : node.data.trimEnd().length - 1
    const range = document.createRange()
    range.setStart(node, offset)
    range.setEnd(node, offset + 1)
    const clips: HTMLElement[] = []
    let ancestor = node.parentElement
    while (ancestor) {
      if (
        /(auto|scroll|hidden|clip)/.test(getComputedStyle(ancestor).overflowY)
      ) {
        clips.push(ancestor)
        const bounds = ancestor.getBoundingClientRect()
        const text = range.getBoundingClientRect()
        const top = bounds.top + ancestor.clientTop
        const bottom = top + ancestor.clientHeight
        if (/(auto|scroll)/.test(getComputedStyle(ancestor).overflowY)) {
          ancestor.scrollBy({
            top:
              text.top < top
                ? text.top - top
                : Math.max(0, text.bottom - bottom),
            behavior: "instant",
          })
        }
      }
      ancestor = ancestor.parentElement
    }
    await settleScrolling(dialog)
    const text = range.getBoundingClientRect()
    expect(text.height).toBeGreaterThan(0)
    expect(text.top).toBeGreaterThanOrEqual(0)
    expect(text.bottom).toBeLessThanOrEqual(window.innerHeight)
    for (const clip of clips) {
      const bounds = clip.getBoundingClientRect()
      expect(text.top).toBeGreaterThanOrEqual(bounds.top + clip.clientTop - 1)
      expect(text.bottom).toBeLessThanOrEqual(
        bounds.top + clip.clientTop + clip.clientHeight + 1
      )
      expect(text.left).toBeGreaterThanOrEqual(
        bounds.left + clip.clientLeft - 1
      )
      expect(text.right).toBeLessThanOrEqual(
        bounds.left + clip.clientLeft + clip.clientWidth + 1
      )
    }
    expectPrivateHelpControlsInView(dialog)
  }
}

export async function readAttemptContextEndsInView(dialog: HTMLDialogElement) {
  for (const selector of [
    '[data-test="excluded-attempt"] .whitespace-pre-wrap',
    '[data-test="stem"] .ql-editor',
    ".is-selected",
  ]) {
    await readContentEndsInView(
      dialog,
      dialog.querySelector<HTMLElement>(selector)!
    )
  }
}

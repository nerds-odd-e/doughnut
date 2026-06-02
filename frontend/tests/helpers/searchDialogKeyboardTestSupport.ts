import { fireEvent } from "@testing-library/vue"
import { expect } from "vitest"

export function testIdSelector(testId: string): string {
  return `[data-testid="${testId}"]`
}

export function dispatchArrowKey(
  key: "ArrowDown" | "ArrowUp",
  target?: Element | null
) {
  const el = target ?? document.activeElement
  expect(el).toBeTruthy()
  fireEvent.keyDown(el!, { key, code: key })
}

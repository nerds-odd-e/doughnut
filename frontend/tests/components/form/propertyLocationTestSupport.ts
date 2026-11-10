import type { VueWrapper } from "@vue/test-utils"
import { expect, vi } from "vitest"
import { propertyRowSelector } from "./propertiesTestDom"

export function spyOnRouter(wrapper: VueWrapper) {
  const router = wrapper.vm.$router
  return {
    router,
    replaceSpy: vi.spyOn(router, "replace"),
    pushSpy: vi.spyOn(router, "push"),
  }
}

export function focusedFlag(wrapper: VueWrapper, key: string) {
  return wrapper
    .find(propertyRowSelector(key))
    .attributes("data-property-focused")
}

export function expectFocusedPropertyValue(row: Element) {
  const value = row.querySelector(
    '[data-testid="rich-note-property-value-frame"]'
  ) as HTMLElement
  expect(value.dataset.propertyValueFocused).toBe("true")
  const style = getComputedStyle(value)
  expect(style.borderInlineStartWidth).toBe("3px")
  expect(style.backgroundColor).not.toBe("rgba(0, 0, 0, 0)")
  expect(style.borderInlineStartColor).not.toBe("rgba(0, 0, 0, 0)")
  const bounds = value.getBoundingClientRect()
  expect(bounds.width).toBeGreaterThan(0)
  expect(bounds.height).toBeGreaterThan(0)
  expect(bounds.left).toBeGreaterThan(row.getBoundingClientRect().left)
  return value
}

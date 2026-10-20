import type { VueWrapper } from "@vue/test-utils"
import { vi } from "vitest"
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

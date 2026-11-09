import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { expect, vi } from "vitest"
import { page } from "vitest/browser"
import { pageBarEl } from "./notebookWorkspaceHeaderTestSupport"

export const activity = () =>
  pageBarEl().querySelector<HTMLButtonElement>(".nav-item button")!
export const expectActivity = async (label: string) =>
  vi.waitFor(() => {
    expect(activity().getAttribute("aria-label")).toBe(label)
    expect(activity().checkVisibility()).toBe(true)
  })
export const click = async (selector: string) => {
  await page.elementLocator(document.querySelector(selector)!).click()
  await flushPromises()
}
export const propertyNote = (readonly = false) =>
  makeMe.aNoteRealm
    .content("---\ntopic: training\nnote_level: 1\n---\n\nBody.")
    .readonly(readonly)
    .please()

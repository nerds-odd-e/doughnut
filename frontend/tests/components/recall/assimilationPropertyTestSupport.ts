import type { Note } from "@generated/doughnut-backend-api"
import { assimilateButtonSelector } from "./assimilationPanelTestSupport"
import { flushPromises } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import { expect } from "vitest"

export const noteWithAssimilationProperties: Note = makeMe.aNoteRealm
  .content(
    `---
topic: micronutrients
url: https://example.com
---

Vitamin notes body.`
  )
  .please().note

export async function expandAssimilationPropertiesSection() {
  const toggle = document.querySelector(
    '[data-test="assimilation-properties-toggle"]'
  ) as HTMLInputElement
  expect(toggle).not.toBeNull()
  toggle.checked = true
  toggle.dispatchEvent(new Event("change", { bubbles: true }))
  await flushPromises()
}

export function assimilationPropertyRow(propertyKey: string) {
  return document.querySelector(
    `[data-test="assimilation-property-row"][data-property-key="${propertyKey}"]`
  )!
}

export async function clickPropertyAssimilate(propertyKey: string) {
  const assimilate = assimilationPropertyRow(propertyKey).querySelector(
    assimilateButtonSelector
  ) as HTMLInputElement
  assimilate.click()
  await flushPromises()
}

import { CUSTOM_RELATION_RADIO_SENTINEL } from "@/models/relationTypeOptions"
import { flushPromises } from "@vue/test-utils"

export const RELATION_SCOPE = "rich-note-relation-property"

export function relationTypeButtonEl(root: ParentNode): HTMLButtonElement {
  const el = root.querySelector(
    '[aria-label="Relation Type"]'
  ) as HTMLButtonElement | null
  expect(el).not.toBeNull()
  return el!
}

export function relationTypeButtonText(root: ParentNode): string {
  return relationTypeButtonEl(root).textContent ?? ""
}

export async function openRelationDialog(editorRoot: ParentNode) {
  relationTypeButtonEl(editorRoot).click()
  await flushPromises()
}

export function customRelationRadioEl(
  dialogRoot: ParentNode = document
): HTMLInputElement {
  const el = dialogRoot.querySelector(
    `input[value="${CUSTOM_RELATION_RADIO_SENTINEL}"]`
  ) as HTMLInputElement | null
  expect(el).not.toBeNull()
  return el!
}

export async function selectCustomRelationRadio(
  dialogRoot: ParentNode = document
) {
  const radio = customRelationRadioEl(dialogRoot)
  radio.checked = true
  radio.dispatchEvent(new Event("change", { bubbles: true }))
  await flushPromises()
}

export function customRelationTextInputEl(
  dialogRoot: ParentNode = document
): HTMLInputElement {
  const el = dialogRoot.querySelector(
    "dialog input[type='text'].daisy-input"
  ) as HTMLInputElement | null
  expect(el).not.toBeNull()
  return el!
}

export function customRelationRadioLabelEl(
  dialogRoot: ParentNode = document
): HTMLLabelElement {
  const el = dialogRoot.querySelector(
    `label[for="${RELATION_SCOPE}-${CUSTOM_RELATION_RADIO_SENTINEL}"]`
  ) as HTMLLabelElement | null
  expect(el).not.toBeNull()
  return el!
}

export async function commitCustomRelationText(
  text: string,
  dialogRoot: ParentNode = document
) {
  const input = customRelationTextInputEl(dialogRoot)
  input.value = text
  input.dispatchEvent(new Event("input", { bubbles: true }))
  input.dispatchEvent(
    new KeyboardEvent("keydown", { key: "Enter", bubbles: true })
  )
  await flushPromises()
}

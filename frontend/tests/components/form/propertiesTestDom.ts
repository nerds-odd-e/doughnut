import { flushPromises, type VueWrapper } from "@vue/test-utils"

export function propertyRowSelector(key: string): string {
  return `[data-testid="rich-note-property-row"][data-property-key="${key}"]`
}

export function propertyRows(root: ParentNode): HTMLElement[] {
  return Array.from(
    root.querySelectorAll('[data-testid="rich-note-property-row"]')
  ) as HTMLElement[]
}

export function propertyPanelToggleEl(row: ParentNode): HTMLButtonElement {
  const el = row.querySelector(
    '[data-testid="rich-note-property-panel-toggle"]'
  ) as HTMLButtonElement | null
  expect(el).not.toBeNull()
  return el!
}

export function propertyPanelEl(row: ParentNode): HTMLElement | null {
  return row.querySelector('[data-testid="rich-note-property-panel"]')
}

export function expectPropertyPanelOpen(row: ParentNode) {
  expect(propertyPanelEl(row)).not.toBeNull()
  expect(propertyPanelToggleEl(row).getAttribute("aria-expanded")).toBe("true")
}

export function expectPropertyPanelClosed(row: ParentNode) {
  expect(propertyPanelEl(row)).toBeNull()
  expect(propertyPanelToggleEl(row).getAttribute("aria-expanded")).toBe("false")
}

type PropertyPanelToggleWrapper = {
  find: (selector: string) => {
    attributes: (name: string) => string | undefined
    trigger: (event: string) => Promise<void>
  }
}

async function setPropertyPanelExpanded(
  wrapper: PropertyPanelToggleWrapper,
  rowSelector: string,
  expanded: boolean
): Promise<void> {
  const toggle = wrapper.find(
    `${rowSelector} [data-testid="rich-note-property-panel-toggle"]`
  )
  if ((toggle.attributes("aria-expanded") === "true") === expanded) {
    return
  }
  await toggle.trigger("click")
  await flushPromises()
}

export async function expandPropertyPanel(
  wrapper: PropertyPanelToggleWrapper,
  rowSelector: string
): Promise<void> {
  await setPropertyPanelExpanded(wrapper, rowSelector, true)
}

export async function collapsePropertyPanel(
  wrapper: PropertyPanelToggleWrapper,
  rowSelector: string
): Promise<void> {
  await setPropertyPanelExpanded(wrapper, rowSelector, false)
}

export async function expandPropertyPanelAndClickRemove(
  wrapper: PropertyPanelToggleWrapper,
  rowSelector: string
): Promise<void> {
  await expandPropertyPanel(wrapper, rowSelector)
  await wrapper
    .find(`${rowSelector} [data-testid="rich-note-property-row-remove"]`)
    .trigger("click")
  await flushPromises()
}

export function propertyRowKeyInputEl(row: ParentNode): HTMLInputElement {
  const el = row.querySelector(
    '[data-testid="rich-note-property-row-key-input"]'
  ) as HTMLInputElement | null
  expect(el).not.toBeNull()
  return el!
}

export function propertyValidationText(root: ParentNode): string {
  const el = root.querySelector('[data-testid="rich-note-property-validation"]')
  expect(el).not.toBeNull()
  return el!.textContent ?? ""
}

export async function triggerRowKeyBlurValidation(wrapper: VueWrapper) {
  const keyInput = wrapper.find(
    '[data-testid="rich-note-property-row-key-input"]'
  )
  await keyInput.trigger("focus")
  await keyInput.trigger("blur")
  await flushPromises()
}

export function propertyRowListValue(wrapper: VueWrapper, key: string) {
  const row = wrapper
    .findAll('[data-testid="rich-note-property-row"]')
    .find((r) => (r.element as HTMLElement).dataset.propertyKey === key)
  expect(row).toBeDefined()
  return row!.find('[data-testid="rich-note-property-row-list-value"]')
}

export function deadWikiLinkInPropertyValueEl(
  root: ParentNode
): HTMLAnchorElement {
  const val = root.querySelector(
    '[data-testid="rich-note-property-row-value-input"]'
  )
  const dead = val?.querySelector(
    "a.dead-wiki-link"
  ) as HTMLAnchorElement | null
  expect(dead).not.toBeNull()
  return dead!
}

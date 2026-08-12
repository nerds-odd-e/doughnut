export function propertyRowSelector(key: string): string {
  return `[data-testid="rich-note-property-row"][data-property-key="${key}"]`
}

export function propertyRows(root: ParentNode): HTMLElement[] {
  return Array.from(
    root.querySelectorAll('[data-testid="rich-note-property-row"]')
  ) as HTMLElement[]
}

export function propertyRowOptionsToggleEl(row: ParentNode): HTMLButtonElement {
  const el = row.querySelector(
    '[data-testid="rich-note-property-row-options-toggle"]'
  ) as HTMLButtonElement | null
  expect(el).not.toBeNull()
  return el!
}

export function propertyRowOptionsPanelEl(row: ParentNode): HTMLElement | null {
  return row.querySelector('[data-testid="rich-note-property-row-options"]')
}

export async function expandPropertyRowOptions(
  wrapper: {
    find: (selector: string) => {
      trigger: (event: string) => Promise<void>
    }
  },
  rowSelector: string
): Promise<void> {
  await wrapper
    .find(
      `${rowSelector} [data-testid="rich-note-property-row-options-toggle"]`
    )
    .trigger("click")
}

export async function expandAndClickPropertyRowRemove(
  wrapper: {
    find: (selector: string) => {
      trigger: (event: string) => Promise<void>
    }
  },
  rowSelector: string
): Promise<void> {
  await expandPropertyRowOptions(wrapper, rowSelector)
  await wrapper
    .find(`${rowSelector} [data-testid="rich-note-property-row-remove"]`)
    .trigger("click")
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

export function propertyRows(root: ParentNode): HTMLElement[] {
  return Array.from(
    root.querySelectorAll('[data-testid="rich-note-property-row"]')
  ) as HTMLElement[]
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
  const dead = val?.querySelector("a.dead-link") as HTMLAnchorElement | null
  expect(dead).not.toBeNull()
  return dead!
}

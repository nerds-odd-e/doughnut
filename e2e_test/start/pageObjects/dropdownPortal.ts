export const dropdownPortalPanelSelector = '[data-dropdown-portal-panel]'

export function findDropdownPortalButton(name: string) {
  return cy
    .get(dropdownPortalPanelSelector)
    .should('be.visible')
    .findByRole('button', { name })
}

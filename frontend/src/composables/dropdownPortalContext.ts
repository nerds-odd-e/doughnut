import type { InjectionKey, Ref } from "vue"

export type DropdownPortalContext = {
  portalId: string
  open: Ref<boolean>
  detailsRef: Ref<HTMLDetailsElement | null>
}

export const dropdownPortalContextKey: InjectionKey<DropdownPortalContext> =
  Symbol("dropdownPortalContext")

export const DROPDOWN_PORTAL_PANEL_ATTR = "data-dropdown-portal-panel"

export function dropdownPortalPanelSelector(portalId: string): string {
  return `[data-dropdown-portal-for="${CSS.escape(portalId)}"]`
}

export function isWithinDropdownPortalPanel(
  target: EventTarget | null
): boolean {
  if (!(target instanceof HTMLElement)) return false
  return target.closest(`[${DROPDOWN_PORTAL_PANEL_ATTR}]`) != null
}

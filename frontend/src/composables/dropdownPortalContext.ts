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

/**
 * Native modal dialogs use the top layer. Portaling to `body` places the panel
 * behind that dialog; portal into the dialog when the trigger is inside one.
 */
export function dropdownPortalTeleportTarget(
  trigger: Element | null | undefined
): HTMLElement | string {
  return trigger?.closest("dialog") ?? "body"
}

/** Lucide icons and other controls often report SVG targets, not HTMLElement. */
export function eventClickTarget(target: EventTarget | null): Element | null {
  return target instanceof Element ? target : null
}

export function isWithinDropdownPortalPanel(
  target: EventTarget | null
): boolean {
  return (
    eventClickTarget(target)?.closest(`[${DROPDOWN_PORTAL_PANEL_ATTR}]`) != null
  )
}

export function isWithinAutoCollapseDropdownTree(
  target: EventTarget | null
): boolean {
  const element = eventClickTarget(target)
  if (element?.closest("[data-auto-collapse-dropdown]")) {
    return true
  }
  return isWithinDropdownPortalPanel(target)
}

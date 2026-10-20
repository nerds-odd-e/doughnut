import type { InjectionKey, Ref } from "vue"

export type DropdownPortalContext = {
  portalId: string
  open: Ref<boolean>
  detailsRef: Ref<HTMLDetailsElement | null>
}

export const dropdownPortalContextKey: InjectionKey<DropdownPortalContext> =
  Symbol("dropdownPortalContext")

const portalPanelForAttribute = "data-dropdown-portal-for"

export function dropdownPortalPanelSelector(portalId: string): string {
  return `[${portalPanelForAttribute}="${CSS.escape(portalId)}"]`
}

/** The portal id of the portaled panel containing `element`, if any. */
export function enclosingPortalPanelId(
  element: Element | null
): string | undefined {
  return (
    element
      ?.closest(`[${portalPanelForAttribute}]`)
      ?.getAttribute(portalPanelForAttribute) ?? undefined
  )
}

export function dropdownPortalOwnerSelector(portalId: string): string {
  return `[data-dropdown-portal-owner="${CSS.escape(portalId)}"]`
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

const DROPDOWN_PORTAL_GAP_PX = 8

const LG_MEDIA_QUERY = "(min-width: 1024px)"

export type DropdownPortalSide = "top" | "bottom" | "left" | "right"

export type DropdownPortalPlacement = {
  side: DropdownPortalSide
  align: "start" | "end"
}

function detailsHasClass(
  details: HTMLDetailsElement,
  className: string
): boolean {
  return details.classList.contains(className)
}

function detailsHasPlacementClass(
  details: HTMLDetailsElement,
  name: "top" | "bottom" | "start" | "end" | "left" | "right",
  options?: { lgOnly?: boolean; baseOnly?: boolean }
): boolean {
  const baseClass = `daisy-dropdown-${name}`
  const lgClass = `lg:${baseClass}`

  if (options?.lgOnly) {
    return (
      window.matchMedia(LG_MEDIA_QUERY).matches &&
      detailsHasClass(details, lgClass)
    )
  }

  if (options?.baseOnly) {
    return detailsHasClass(details, baseClass)
  }

  if (detailsHasClass(details, baseClass)) return true
  return (
    window.matchMedia(LG_MEDIA_QUERY).matches &&
    detailsHasClass(details, lgClass)
  )
}

function resolveSide(details: HTMLDetailsElement): DropdownPortalSide {
  if (window.matchMedia(LG_MEDIA_QUERY).matches) {
    if (detailsHasPlacementClass(details, "right", { lgOnly: true })) {
      return "right"
    }
    if (detailsHasPlacementClass(details, "left", { lgOnly: true })) {
      return "left"
    }
    if (detailsHasPlacementClass(details, "top", { lgOnly: true })) {
      return "top"
    }
    if (detailsHasPlacementClass(details, "bottom", { lgOnly: true })) {
      return "bottom"
    }
  }

  if (detailsHasPlacementClass(details, "right", { baseOnly: true })) {
    return "right"
  }
  if (detailsHasPlacementClass(details, "left", { baseOnly: true })) {
    return "left"
  }
  if (detailsHasPlacementClass(details, "top", { baseOnly: true })) {
    return "top"
  }
  return "bottom"
}

export function parseDropdownPlacementFromDetails(
  details: HTMLDetailsElement | null
): DropdownPortalPlacement {
  if (!details) {
    return { side: "bottom", align: "start" }
  }

  const side = resolveSide(details)
  const align = detailsHasPlacementClass(details, "end") ? "end" : "start"
  return { side, align }
}

function crossAxisPosition(
  anchorStart: number,
  anchorEnd: number,
  size: number,
  align: "start" | "end"
): number {
  return align === "end" ? anchorEnd - size : anchorStart
}

export function computeDropdownPortalStyle(
  anchorRect: DOMRect,
  panelWidth: number,
  panelHeight: number,
  placement: DropdownPortalPlacement
): { top: string; left: string } {
  const gap = DROPDOWN_PORTAL_GAP_PX
  const { side, align } = placement

  switch (side) {
    case "right":
      return {
        top: `${crossAxisPosition(anchorRect.top, anchorRect.bottom, panelHeight, align)}px`,
        left: `${anchorRect.right + gap}px`,
      }
    case "left":
      return {
        top: `${crossAxisPosition(anchorRect.top, anchorRect.bottom, panelHeight, align)}px`,
        left: `${anchorRect.left - panelWidth - gap}px`,
      }
    case "top":
      return {
        top: `${anchorRect.top - panelHeight - gap}px`,
        left: `${crossAxisPosition(anchorRect.left, anchorRect.right, panelWidth, align)}px`,
      }
    default:
      return {
        top: `${anchorRect.bottom + gap}px`,
        left: `${crossAxisPosition(anchorRect.left, anchorRect.right, panelWidth, align)}px`,
      }
  }
}

export const dropdownPortalPanelFallbackSize = {
  width: 208,
  height: 120,
} as const

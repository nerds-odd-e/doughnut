const DROPDOWN_PORTAL_GAP_PX = 8

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

function resolveSide(details: HTMLDetailsElement): DropdownPortalSide {
  if (detailsHasClass(details, "daisy-dropdown-right")) return "right"
  if (detailsHasClass(details, "daisy-dropdown-left")) return "left"
  if (detailsHasClass(details, "daisy-dropdown-top")) return "top"
  return "bottom"
}

export function parseDropdownPlacementFromDetails(
  details: HTMLDetailsElement | null
): DropdownPortalPlacement {
  if (!details) {
    return { side: "bottom", align: "start" }
  }

  const side = resolveSide(details)
  const align = detailsHasClass(details, "daisy-dropdown-end") ? "end" : "start"
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

function leftInsideScreen(
  anchorRect: DOMRect,
  panelWidth: number,
  align: "start" | "end"
): number {
  const gap = DROPDOWN_PORTAL_GAP_PX
  const left = crossAxisPosition(
    anchorRect.left,
    anchorRect.right,
    panelWidth,
    align
  )
  const rightmost = document.documentElement.clientWidth - panelWidth - gap
  return Math.max(gap, Math.min(left, rightmost))
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
        left: `${leftInsideScreen(anchorRect, panelWidth, align)}px`,
      }
    default:
      return {
        top: `${anchorRect.bottom + gap}px`,
        left: `${leftInsideScreen(anchorRect, panelWidth, align)}px`,
      }
  }
}

export const dropdownPortalPanelFallbackSize = {
  width: 208,
  height: 120,
} as const

import {
  ArrowDownAZ,
  ArrowUpAZ,
  CalendarArrowDown,
  CalendarArrowUp,
  ClockArrowDown,
  ClockArrowUp,
} from "@lucide/vue"
import type { PeerSortSpec } from "@/composables/usePeerSort"

export type PeerSortMenuRow = {
  spec: PeerSortSpec
  label: string
  Icon: typeof ArrowDownAZ
}

export const PEER_SORT_MENU_ROWS: PeerSortMenuRow[] = [
  {
    spec: { field: "title", direction: "asc" },
    label: "Title (A–Z)",
    Icon: ArrowDownAZ,
  },
  {
    spec: { field: "title", direction: "desc" },
    label: "Title (Z–A)",
    Icon: ArrowUpAZ,
  },
  {
    spec: { field: "created", direction: "asc" },
    label: "Created (oldest first)",
    Icon: CalendarArrowDown,
  },
  {
    spec: { field: "created", direction: "desc" },
    label: "Created (newest first)",
    Icon: CalendarArrowUp,
  },
  {
    spec: { field: "updated", direction: "asc" },
    label: "Updated (oldest first)",
    Icon: ClockArrowDown,
  },
  {
    spec: { field: "updated", direction: "desc" },
    label: "Updated (newest first)",
    Icon: ClockArrowUp,
  },
]

function peerSortMenuRowMatching(spec: PeerSortSpec) {
  return PEER_SORT_MENU_ROWS.find(
    (row) =>
      row.spec.field === spec.field && row.spec.direction === spec.direction
  )
}

export function peerSortTriggerIcon(spec: PeerSortSpec) {
  return peerSortMenuRowMatching(spec)?.Icon ?? ArrowDownAZ
}

import {
  ArrowDownAZ,
  ArrowUpAZ,
  CalendarArrowDown,
  CalendarArrowUp,
  ClockArrowDown,
  ClockArrowUp,
} from "lucide-vue-next"
import type { SidebarPeerSortSpec } from "@/composables/useNoteSidebarPeerSort"

export type SidebarPeerSortMenuRow = {
  spec: SidebarPeerSortSpec
  label: string
  Icon: typeof ArrowDownAZ
}

export const SIDEBAR_PEER_SORT_MENU_ROWS: SidebarPeerSortMenuRow[] = [
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

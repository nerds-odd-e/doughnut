import { ref, watch } from "vue"

export type SidebarPeerSortField = "title" | "created" | "updated"
export type SidebarPeerSortDirection = "asc" | "desc"

export type SidebarPeerSortSpec = {
  field: SidebarPeerSortField
  direction: SidebarPeerSortDirection
}

export const NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY =
  "doughnut.noteSidebar.peerSort"

const defaultSpec: SidebarPeerSortSpec = {
  field: "title",
  direction: "asc",
}

function parseStoredSpec(raw: string | null): SidebarPeerSortSpec | undefined {
  if (raw == null || raw === "") return undefined
  try {
    const v = JSON.parse(raw) as unknown
    if (v == null || typeof v !== "object") return undefined
    const field = (v as { field?: unknown }).field
    const direction = (v as { direction?: unknown }).direction
    if (
      (field === "title" || field === "created" || field === "updated") &&
      (direction === "asc" || direction === "desc")
    ) {
      return { field, direction }
    }
  } catch {
    /* ignore invalid JSON */
  }
  return undefined
}

function readStoredSpec(): SidebarPeerSortSpec {
  if (typeof localStorage === "undefined") return defaultSpec
  return (
    parseStoredSpec(localStorage.getItem(NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY)) ??
    defaultSpec
  )
}

const sortPeerSpec = ref<SidebarPeerSortSpec>(readStoredSpec())

watch(
  sortPeerSpec,
  (spec) => {
    if (typeof localStorage === "undefined") return
    localStorage.setItem(
      NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY,
      JSON.stringify(spec)
    )
  },
  { deep: true }
)

export function useNoteSidebarPeerSort() {
  const stored = readStoredSpec()
  if (
    sortPeerSpec.value.field !== stored.field ||
    sortPeerSpec.value.direction !== stored.direction
  ) {
    sortPeerSpec.value = stored
  }

  function setSortPeerSpec(spec: SidebarPeerSortSpec) {
    sortPeerSpec.value = spec
  }

  return { sortPeerSpec, setSortPeerSpec }
}

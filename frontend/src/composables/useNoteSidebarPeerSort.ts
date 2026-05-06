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

function readSpecFromSession(): SidebarPeerSortSpec {
  if (typeof sessionStorage === "undefined") return defaultSpec
  try {
    const raw = sessionStorage.getItem(NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY)
    if (raw == null || raw === "") return defaultSpec
    const v = JSON.parse(raw) as unknown
    if (v == null || typeof v !== "object") return defaultSpec
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
  return defaultSpec
}

const sortPeerSpec = ref<SidebarPeerSortSpec>(readSpecFromSession())

watch(
  sortPeerSpec,
  (spec) => {
    if (typeof sessionStorage === "undefined") return
    sessionStorage.setItem(
      NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY,
      JSON.stringify(spec)
    )
  },
  { deep: true }
)

export function useNoteSidebarPeerSort() {
  const fromSession = readSpecFromSession()
  if (
    sortPeerSpec.value.field !== fromSession.field ||
    sortPeerSpec.value.direction !== fromSession.direction
  ) {
    sortPeerSpec.value = fromSession
  }

  function setSortPeerSpec(spec: SidebarPeerSortSpec) {
    sortPeerSpec.value = spec
  }

  return { sortPeerSpec, setSortPeerSpec }
}

import { ref, watch } from "vue"

export type PeerSortField = "title" | "created" | "updated"
export type PeerSortDirection = "asc" | "desc"

export type PeerSortSpec = {
  field: PeerSortField
  direction: PeerSortDirection
}

export const PEER_SORT_STORAGE_KEY = "donut.noteSidebar.peerSort"

const defaultSpec: PeerSortSpec = {
  field: "title",
  direction: "asc",
}

function parseStoredSpec(raw: string | null): PeerSortSpec | undefined {
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

function readStoredSpec(): PeerSortSpec {
  if (typeof localStorage === "undefined") return defaultSpec
  return (
    parseStoredSpec(localStorage.getItem(PEER_SORT_STORAGE_KEY)) ?? defaultSpec
  )
}

const peerSortSpec = ref<PeerSortSpec>(readStoredSpec())

watch(
  peerSortSpec,
  (spec) => {
    if (typeof localStorage === "undefined") return
    localStorage.setItem(PEER_SORT_STORAGE_KEY, JSON.stringify(spec))
  },
  { deep: true }
)

export function usePeerSort() {
  const stored = readStoredSpec()
  if (
    peerSortSpec.value.field !== stored.field ||
    peerSortSpec.value.direction !== stored.direction
  ) {
    peerSortSpec.value = stored
  }

  function setPeerSortSpec(spec: PeerSortSpec) {
    peerSortSpec.value = spec
  }

  return { peerSortSpec, setPeerSortSpec }
}

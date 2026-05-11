import type { NoteRealm } from "@generated/doughnut-backend-api"

/** Leaf folder for placement UI: last segment of `ancestorFolders` (notebook root when absent). */
export function realmLeafFolder(realm: NoteRealm | undefined) {
  const chain = realm?.ancestorFolders
  if (chain == null || chain.length === 0) return
  return chain[chain.length - 1]
}

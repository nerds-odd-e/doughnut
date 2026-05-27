import { ref } from "vue"
import { invalidateSidebarListingCache } from "./sidebarFolderListingCache"

export const sidebarStructuralRefreshKey = ref(0)

export function refreshSidebarStructuralListings() {
  invalidateSidebarListingCache()
  sidebarStructuralRefreshKey.value += 1
}

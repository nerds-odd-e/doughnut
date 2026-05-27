import type { FolderListing } from "@generated/doughnut-backend-api"

const cache = new Map<string, FolderListing>()

function key(notebookId: number, folderId: number | null): string {
  return `${notebookId}:${folderId ?? "root"}`
}

export function getCachedListing(
  notebookId: number,
  folderId: number | null
): FolderListing | undefined {
  return cache.get(key(notebookId, folderId))
}

export function setCachedListing(
  notebookId: number,
  folderId: number | null,
  listing: FolderListing
): void {
  cache.set(key(notebookId, folderId), listing)
}

export function invalidateSidebarListingCache(): void {
  cache.clear()
}

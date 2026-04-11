export type BookBlockRowForLiveAnnouncement = {
  title: string
  id: number
}

export function structuralTitleForBlockId(
  blockId: number | null,
  rows: BookBlockRowForLiveAnnouncement[]
): string {
  if (blockId === null) {
    return ""
  }
  const row = rows.find((r) => r.id === blockId)
  return row?.title ?? ""
}

export function nextLiveAnnouncementText(
  previousAnnouncedTitle: string | undefined,
  blockId: number | null,
  rows: BookBlockRowForLiveAnnouncement[]
): { text: string; changed: boolean } {
  const resolved = structuralTitleForBlockId(blockId, rows)
  if (resolved === previousAnnouncedTitle) {
    return { text: resolved, changed: false }
  }
  return { text: resolved, changed: true }
}

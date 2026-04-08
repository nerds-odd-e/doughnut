export type BookRangeRowForLiveAnnouncement = {
  title: string
  startAnchor: { id: number }
}

export function structuralTitleForStartAnchorId(
  anchorId: number | null,
  rows: BookRangeRowForLiveAnnouncement[]
): string {
  if (anchorId === null) {
    return ""
  }
  const row = rows.find((r) => r.startAnchor.id === anchorId)
  return row?.title ?? ""
}

export function nextLiveAnnouncementText(
  previousAnnouncedTitle: string | undefined,
  anchorId: number | null,
  rows: BookRangeRowForLiveAnnouncement[]
): { text: string; changed: boolean } {
  const resolved = structuralTitleForStartAnchorId(anchorId, rows)
  if (resolved === previousAnnouncedTitle) {
    return { text: resolved, changed: false }
  }
  return { text: resolved, changed: true }
}

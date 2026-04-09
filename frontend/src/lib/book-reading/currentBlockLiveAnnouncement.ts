export type BookBlockRowForLiveAnnouncement = {
  title: string
  startAnchor: { id: number }
}

export function structuralTitleForStartAnchorId(
  anchorId: number | null,
  rows: BookBlockRowForLiveAnnouncement[]
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
  rows: BookBlockRowForLiveAnnouncement[]
): { text: string; changed: boolean } {
  const resolved = structuralTitleForStartAnchorId(anchorId, rows)
  if (resolved === previousAnnouncedTitle) {
    return { text: resolved, changed: false }
  }
  return { text: resolved, changed: true }
}

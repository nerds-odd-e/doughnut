import type { NoteRealm } from "@generated/doughnut-backend-api"

/** Titles used to expand matching folder rows for the active note (folder names + active note title). */
export function structuralSidebarTitlesFromRealm(
  noteRealm: NoteRealm | undefined
): Set<string> {
  const titles = new Set<string>()
  if (noteRealm == null) return titles
  const noteTitle = noteRealm.note?.noteTopology?.title
  if (noteTitle != null && noteTitle !== "") {
    titles.add(noteTitle)
  }
  for (const seg of noteRealm.ancestorFolders ?? []) {
    if (seg.name != null && seg.name !== "") {
      titles.add(seg.name)
    }
  }
  return titles
}

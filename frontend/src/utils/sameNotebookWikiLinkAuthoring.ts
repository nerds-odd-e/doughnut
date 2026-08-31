import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { markdownWikiTokenFromDeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"

/** Owns the backend-authored Portable-path spelling for an insert (same- or cross-notebook, qualified by the backend) or ambiguous-shorthand repair. */
async function authoredPortablePathFor(
  sourceNoteId: number,
  destinationNoteId: number,
  originalPortablePath?: string
): Promise<string | undefined> {
  const { data, error } = await apiCallWithLoading(() =>
    NoteController.authoredPortablePath({
      path: { note: sourceNoteId },
      query: {
        destinationNote: destinationNoteId,
        ...(originalPortablePath === undefined
          ? {}
          : { portablePath: originalPortablePath }),
      },
    })
  )
  if (error || !data) return
  return data.portablePath
}

/** Insert: shortest unambiguous Portable path (cross-notebook qualified by the backend), wrapped as a wiki-link token. */
export async function authoredWikiLinkTokenForInsert(
  sourceNoteId: number,
  destinationNoteId: number
): Promise<string | undefined> {
  const portablePath = await authoredPortablePathFor(
    sourceNoteId,
    destinationNoteId
  )
  if (portablePath === undefined) return
  return `[[${portablePath}]]`
}

/**
 * Backend-authored path for a known original reference (an ambiguous shorthand being
 * repaired, or a pasted link being converted), preserving its display text — or the
 * destination's own `#prop:` suffix when there was no display text to preserve.
 */
export async function authoredWikiLinkTokenFromOriginalPath(
  sourceNoteId: number,
  destinationNoteId: number,
  originalPortablePath: string,
  displayText?: string
): Promise<string | undefined> {
  const portablePath = await authoredPortablePathFor(
    sourceNoteId,
    destinationNoteId,
    originalPortablePath
  )
  if (portablePath === undefined) return
  return markdownWikiTokenFromDeadWikiLinkPayload({
    portablePath,
    displayText: displayText ?? portablePath,
  })
}

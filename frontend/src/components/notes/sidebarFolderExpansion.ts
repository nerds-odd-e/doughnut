import type { ComputedRef, InjectionKey, Ref } from "vue"

export type ExpandedFolderSlugSet = Ref<Set<string>>

export const sidebarExpandedFolderSlugsKey: InjectionKey<ExpandedFolderSlugSet> =
  Symbol("sidebarExpandedFolderSlugs")

export const sidebarToggleFolderSlugKey: InjectionKey<(slug: string) => void> =
  Symbol("sidebarToggleFolderSlug")

export const sidebarActiveNoteFolderSlugPrefixesKey: InjectionKey<
  ComputedRef<Set<string>>
> = Symbol("sidebarActiveNoteFolderSlugPrefixes")

/** Structural folder slug path segments toward a note slug (excluding the note basename segment). */
export function noteSlugFolderPrefixes(noteSlug: string): string[] {
  const parts = noteSlug.split("/").filter(Boolean)
  if (parts.length <= 1) return []
  const prefixes: string[] = []
  for (let depth = 1; depth < parts.length; depth++) {
    prefixes.push(parts.slice(0, depth).join("/"))
  }
  return prefixes
}

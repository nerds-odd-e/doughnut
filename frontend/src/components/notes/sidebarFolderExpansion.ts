import type { ComputedRef, InjectionKey, Ref } from "vue"

export type ExpandedFolderIdSet = Ref<Set<number>>

export const sidebarExpandedFolderIdsKey: InjectionKey<ExpandedFolderIdSet> =
  Symbol("sidebarExpandedFolderIds")

export const sidebarToggleFolderIdKey: InjectionKey<
  (folderId: number) => void
> = Symbol("sidebarToggleFolderId")

/** Note titles along active note → root topology chain (including the active note). */
export const sidebarStructuralSidebarTitlesKey: InjectionKey<
  ComputedRef<Set<string>>
> = Symbol("sidebarStructuralSidebarTitles")

/** Assigned folder (folder-first containment), when present on topology. */
export const sidebarActiveNoteFolderIdsKey: InjectionKey<
  ComputedRef<Set<number>>
> = Symbol("sidebarActiveNoteFolderIds")

/** User-clicked folder row for default create placement; cleared on blur, note click, or notebook switch. */
export const sidebarUserActiveFolderIdKey: InjectionKey<Ref<number | null>> =
  Symbol("sidebarUserActiveFolderId")

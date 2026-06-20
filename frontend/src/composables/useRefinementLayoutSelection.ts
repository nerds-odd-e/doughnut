import type { NoteRefinementLayoutItem } from "@generated/doughnut-backend-api"
import { computed, ref, type Ref } from "vue"

export function flattenLayoutItems(
  items: NoteRefinementLayoutItem[]
): NoteRefinementLayoutItem[] {
  return items.flatMap((item) => [
    item,
    ...flattenLayoutItems(item.children ?? []),
  ])
}

export function useRefinementLayoutSelection(
  layoutItems: Ref<NoteRefinementLayoutItem[]>
) {
  const allLayoutItems = computed(() => flattenLayoutItems(layoutItems.value))

  const layoutItemsById = computed(
    () => new Map(allLayoutItems.value.map((item) => [item.id, item]))
  )

  const descendantIds = (item: NoteRefinementLayoutItem): string[] =>
    flattenLayoutItems([item]).map(({ id }) => id)

  const selectedItemIds = ref<string[]>([])
  const selectedItemIdSet = computed(() => new Set(selectedItemIds.value))

  const isFullySelected = (item: NoteRefinementLayoutItem) =>
    descendantIds(item).every((id) => selectedItemIdSet.value.has(id))

  const isPartiallySelected = (item: NoteRefinementLayoutItem) => {
    const ids = descendantIds(item)
    return (
      ids.some((id) => selectedItemIdSet.value.has(id)) &&
      ids.some((id) => !selectedItemIdSet.value.has(id))
    )
  }

  const setItemSelection = (
    item: NoteRefinementLayoutItem,
    selected: boolean
  ) => {
    const ids = descendantIds(item)
    if (selected) {
      selectedItemIds.value = Array.from(
        new Set([...selectedItemIds.value, ...ids])
      )
    } else {
      const idsToRemove = new Set(ids)
      selectedItemIds.value = selectedItemIds.value.filter(
        (id) => !idsToRemove.has(id)
      )
    }
  }

  const clearSelection = () => {
    selectedItemIds.value = []
  }

  return {
    selectedItemIds,
    layoutItemsById,
    isFullySelected,
    isPartiallySelected,
    setItemSelection,
    clearSelection,
  }
}

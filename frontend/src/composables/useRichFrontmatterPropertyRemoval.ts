import { listPropertyValue } from "@/utils/noteProperties"
import type { Ref } from "vue"
import { runWithBlockingApiLoading } from "@/managedApi/clientSetup"
import {
  removePropertyRowAt,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"

export function useRichFrontmatterPropertyRemoval(options: {
  propertyRows: Ref<PropertyRow[]>
  confirmAndApplyRemoval: (key: string, value?: string) => Promise<boolean>
  followFocusedPropertyDelete: (key: string) => void | Promise<unknown>
  followFocusedPropertyItemRemoval: (
    key: string,
    previousItems: readonly string[],
    removedIndex: number
  ) => void | Promise<unknown>
  emitProperties: (rows: PropertyRow[]) => void
}) {
  function removeRow(idx: number) {
    return runWithBlockingApiLoading(async () => {
      const key = options.propertyRows.value[idx]?.key ?? ""
      if (!(await options.confirmAndApplyRemoval(key))) return

      options.propertyRows.value = removePropertyRowAt(
        options.propertyRows.value,
        idx
      )
      await options.followFocusedPropertyDelete(key)
      options.emitProperties(options.propertyRows.value)
    }, "Updating property…")
  }

  function removeItem(idx: number, itemIndex: number) {
    const row = options.propertyRows.value[idx]!
    if (row.value.kind !== "list") return
    const previousItems = row.value.items
    if (previousItems.length === 1) return removeRow(idx)
    return runWithBlockingApiLoading(async () => {
      if (
        !(await options.confirmAndApplyRemoval(
          row.key,
          previousItems[itemIndex]!
        ))
      )
        return
      options.propertyRows.value = options.propertyRows.value.map(
        (current, index) =>
          index === idx
            ? {
                ...row,
                value: listPropertyValue(
                  previousItems.filter((_, index) => index !== itemIndex)
                ),
              }
            : current
      )
      await options.followFocusedPropertyItemRemoval(
        row.key,
        previousItems,
        itemIndex
      )
      options.emitProperties(options.propertyRows.value)
    }, "Updating property…")
  }

  return { removeRow, removeItem }
}

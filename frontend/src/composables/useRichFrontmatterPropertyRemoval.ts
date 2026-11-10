import type { Ref } from "vue"
import { runWithBlockingApiLoading } from "@/managedApi/clientSetup"
import {
  removePropertyRowAt,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"

export function useRichFrontmatterPropertyRemoval(options: {
  propertyRows: Ref<PropertyRow[]>
  confirmAndApplyRemoval: (key: string) => Promise<boolean>
  followFocusedPropertyDelete: (key: string) => void | Promise<unknown>
  emitProperties: (rows: PropertyRow[]) => void
}) {
  function removeRow(idx: number) {
    return runWithBlockingApiLoading(async () => {
      const key = options.propertyRows.value[idx]?.key.trim() ?? ""
      if (!(await options.confirmAndApplyRemoval(key))) return

      options.propertyRows.value = removePropertyRowAt(
        options.propertyRows.value,
        idx
      )
      await options.followFocusedPropertyDelete(key)
      options.emitProperties(options.propertyRows.value)
    }, "Updating property…")
  }

  return { removeRow }
}

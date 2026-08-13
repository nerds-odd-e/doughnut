import { ref, watch, type Ref } from "vue"
import type { PropertyRow } from "@/utils/noteContentFrontmatter"

export function usePropertyRowClientIds(propertyRows: Ref<PropertyRow[]>) {
  const rowClientIds = ref<number[]>([])
  let nextRowClientId = 1

  watch(
    propertyRows,
    (rows, prevRows) => {
      const prevIds = rowClientIds.value
      const idsByKey = new Map<string, number[]>()
      if (prevRows) {
        for (let i = 0; i < prevRows.length; i++) {
          const key = prevRows[i]!.key
          const queue = idsByKey.get(key) ?? []
          queue.push(prevIds[i]!)
          idsByKey.set(key, queue)
        }
      }
      const matched = rows.map((row): number | undefined => {
        const queue = idsByKey.get(row.key)
        if (queue?.length) return queue.shift()!
        return
      })
      const leftover = prevIds.filter((id) => !matched.includes(id))
      let leftoverIndex = 0
      rowClientIds.value = matched.map(
        (id) => id ?? leftover[leftoverIndex++] ?? nextRowClientId++
      )
    },
    { immediate: true }
  )

  return rowClientIds
}

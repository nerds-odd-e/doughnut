import { computed, nextTick, type Ref } from "vue"
import { usePropertyMemoryTrackerGuard } from "@/composables/usePropertyMemoryTrackerGuard"
import { relationKebabFromLabel } from "@/models/relationTypeOptions"
import { primeSoftKeyboard } from "@/utils/focusTarget"
import {
  findPropertyRowIndexByExactKey,
  isListCapablePropertyKey,
  isRelationPropertyKey,
  isReservedReadmeOnlyPropertyKey,
  normalizePropertyRowForCommit,
  notePropertiesFromPropertyRows,
  propertyRowForInsertedKey,
  propertyRowWithScalar,
  propertyRowsAfterAppendingValueToExactKey,
  removePropertyRowAt,
  scalarStringFromPropertyRow,
  sortedPropertyRowsFromNoteProperties,
  validatePropertyRowsForRichEdit,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import { scalarPropertyValue } from "@/utils/noteProperties"

export function useRichFrontmatterPropertyEditing(options: {
  propertyRows: Ref<PropertyRow[]>
  noteId: () => number | undefined
  isReadmeContext: () => boolean
  onPropertiesChanged: (rows: PropertyRow[]) => void
  setValidationMessage: (message: string) => void
  clearValidation: () => void
  insertKeyInputId: string
  insertOpen: Ref<boolean>
  draftKey: Ref<string>
  draftValue: Ref<string>
  rowSnapshots: Ref<Record<number, PropertyRow>>
  isReadOnly: () => boolean
  parsedOk: () => boolean
}) {
  const { confirmAndApplyRemoval, confirmAndApplyRename } =
    usePropertyMemoryTrackerGuard(options.noteId)

  function filterForEmit(rows: PropertyRow[]): PropertyRow[] {
    if (!options.isReadmeContext()) return rows
    return rows.filter(
      (r) =>
        !(
          isReservedReadmeOnlyPropertyKey(r.key) &&
          !scalarStringFromPropertyRow(r)?.trim()
        )
    )
  }

  function rowsAfterAdding(row: PropertyRow): PropertyRow[] {
    const properties = notePropertiesFromPropertyRows(
      options.propertyRows.value
    )
    properties[row.key] = row.value
    return sortedPropertyRowsFromNoteProperties(properties)
  }

  async function openPropertyInsert() {
    primeSoftKeyboard()
    options.insertOpen.value = true
    await nextTick()
    requestAnimationFrame(() => {
      document.getElementById(options.insertKeyInputId)?.focus()
    })
  }

  function tryCommitInsert() {
    const key = options.draftKey.value.trim()
    const value = options.draftValue.value.trim()
    if (!key || !value) return

    let nextRows: PropertyRow[]
    if (findPropertyRowIndexByExactKey(options.propertyRows.value, key) >= 0) {
      if (!isListCapablePropertyKey(key)) {
        options.setValidationMessage("Duplicate property keys are not allowed.")
        return
      }
      nextRows = propertyRowsAfterAppendingValueToExactKey(
        options.propertyRows.value,
        key,
        value
      )!
    } else {
      nextRows = rowsAfterAdding(propertyRowForInsertedKey(key, value))
    }

    const result = validatePropertyRowsForRichEdit(nextRows)
    if (!result.ok) {
      options.setValidationMessage(result.message)
      return
    }

    options.clearValidation()
    options.onPropertiesChanged(filterForEmit(nextRows))
  }

  function onRowFocus(idx: number) {
    const row = options.propertyRows.value[idx]
    if (row) {
      options.rowSnapshots.value[idx] = { ...row }
    }
  }

  async function removeRow(idx: number) {
    const key = options.propertyRows.value[idx]?.key.trim() ?? ""
    const proceed = await confirmAndApplyRemoval(key)
    if (!proceed) {
      return
    }

    options.propertyRows.value = removePropertyRowAt(
      options.propertyRows.value,
      idx
    )
    options.clearValidation()
    options.onPropertiesChanged(filterForEmit([...options.propertyRows.value]))
  }

  async function commitRow(idx: number) {
    const snapshot = options.rowSnapshots.value[idx]
    const rows = options.propertyRows.value.map((r, i) =>
      i === idx ? normalizePropertyRowForCommit(r) : r
    )
    options.propertyRows.value = rows

    const result = validatePropertyRowsForRichEdit(options.propertyRows.value)
    if (!result.ok) {
      options.setValidationMessage(result.message)
      if (snapshot) {
        options.propertyRows.value = options.propertyRows.value.map((r, i) =>
          i === idx ? { ...snapshot } : r
        )
      }
      return
    }

    const newKey = rows[idx]?.key ?? ""
    const oldKey = snapshot?.key.trim() ?? ""
    if (oldKey !== "" && oldKey !== newKey) {
      const proceed = await confirmAndApplyRename(oldKey, newKey)
      if (!proceed) {
        if (snapshot) {
          options.propertyRows.value = options.propertyRows.value.map((r, i) =>
            i === idx ? { ...snapshot } : r
          )
        }
        return
      }
    }

    options.clearValidation()
    options.onPropertiesChanged(filterForEmit([...options.propertyRows.value]))
  }

  function onRelationTypeSelected(idx: number, newType: string | undefined) {
    if (newType === undefined) return
    const row = options.propertyRows.value[idx]
    if (!row || !isRelationPropertyKey(row.key)) return
    const current = scalarStringFromPropertyRow(row) ?? ""
    const nextKebab = relationKebabFromLabel(newType)
    if (current.trim().toLowerCase() === nextKebab.toLowerCase()) return
    const rows = options.propertyRows.value.map((r, i) =>
      i === idx
        ? normalizePropertyRowForCommit({
            ...r,
            value: scalarPropertyValue(nextKebab),
          })
        : normalizePropertyRowForCommit(r)
    )
    options.propertyRows.value = rows
    const result = validatePropertyRowsForRichEdit(options.propertyRows.value)
    if (!result.ok) {
      options.setValidationMessage(result.message)
      return
    }
    options.clearValidation()
    options.onPropertiesChanged(filterForEmit([...options.propertyRows.value]))
  }

  async function addWikiLinkAsProperty(wikiLinkText: string) {
    const trimmedLink = wikiLinkText.trim()
    const newRows = [
      ...options.propertyRows.value,
      propertyRowWithScalar("", wikiLinkText),
    ]
    const result = validatePropertyRowsForRichEdit(newRows)
    if (!result.ok) {
      options.setValidationMessage(result.message)
      return
    }
    options.clearValidation()
    options.propertyRows.value = newRows
    options.onPropertiesChanged(filterForEmit([...newRows]))
    await nextTick()
    const idx = options.propertyRows.value.findIndex(
      (r) =>
        !r.key.trim() && scalarStringFromPropertyRow(r)?.trim() === trimmedLink
    )
    const rowIndex = idx >= 0 ? idx : options.propertyRows.value.length - 1
    requestAnimationFrame(() => {
      const el = document.querySelector(
        `[data-testid="rich-note-property-row"][data-row-index="${rowIndex}"] [data-testid="rich-note-property-row-key-input"]`
      ) as HTMLInputElement | null
      el?.focus()
    })
  }

  const getPropertyRows = (): PropertyRow[] =>
    filterForEmit(options.propertyRows.value)

  const headingVisible = computed(
    () => options.propertyRows.value.length > 0 || options.isReadOnly()
  )
  const showSection = computed(() => {
    if (!options.parsedOk()) return false
    if (options.isReadOnly()) return options.propertyRows.value.length > 0
    return true
  })
  const showInsertChrome = computed(
    () => !options.isReadOnly() && options.parsedOk()
  )

  return {
    filterForEmit,
    rowsAfterAdding,
    openPropertyInsert,
    tryCommitInsert,
    onRowFocus,
    removeRow,
    commitRow,
    onRelationTypeSelected,
    addWikiLinkAsProperty,
    getPropertyRows,
    headingVisible,
    showSection,
    showInsertChrome,
  }
}

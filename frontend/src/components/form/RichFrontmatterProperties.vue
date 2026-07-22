<template>
  <section
    v-if="showSection"
    class="mb-3"
    :class="
      isInteractionLocked
        ? 'pointer-events-none opacity-60'
        : ''
    "
    :aria-labelledby="headingVisible ? headingId : undefined"
    :aria-label="headingVisible ? undefined : 'Note properties'"
  >
    <div
      v-if="headingVisible"
      class="flex items-center justify-between gap-2 mb-2"
    >
      <h4
        :id="headingId"
        class="mb-0 text-sm font-semibold"
      >
        Properties
      </h4>
      <button
        v-if="showInsertChrome && propertyRows.length > 0"
        type="button"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm inline-flex shrink-0 items-center gap-1"
        @click="openPropertyInsert"
      >
        <Plus class="h-4 w-4" aria-hidden="true" />
        Add property
      </button>
    </div>
    <RichFrontmatterReadOnlyList
      v-if="propertyRows.length > 0 && isReadOnly"
      :property-rows="propertyRows"
    />
    <div
      v-else-if="propertyRows.length > 0"
      class="flex flex-col gap-2 text-sm"
    >
      <RichFrontmatterEditablePropertyRow
        v-for="(_, idx) in propertyRows"
        :key="idx"
        v-model="propertyRows[idx]!"
        :idx="idx"
        :wiki-titles="wikiTitles"
        :note-id="noteId"
        :property-rows="propertyRows"
        :key-input-id="rowKeyInputId(idx)"
        :preset-list-id="rowKeyPresetListId(idx)"
        @row-focus="onRowFocus(idx)"
        @commit="commitRow(idx)"
        @remove="removeRow(idx)"
        @wikidata-dialog-open="openWikidataDialog({ type: 'row', idx })"
        @dead-link-click="emits('deadLinkClick', $event)"
        @relation-type-selected="onRelationTypeSelected(idx, $event)"
        @image-upload-state="emits('image-upload-state', $event)"
      />
    </div>
    <p
      v-if="validationMessage"
      role="alert"
      aria-live="polite"
      class="text-error text-xs mt-1"
      data-testid="rich-note-property-validation"
    >
      {{ validationMessage }}
    </p>
    <button
      v-if="showInsertChrome && !insertOpen && propertyRows.length === 0"
      type="button"
      class="daisy-btn daisy-btn-ghost daisy-btn-sm inline-flex self-start items-center gap-1"
      @click="openPropertyInsert"
    >
      <Plus class="h-4 w-4" aria-hidden="true" />
      Add property
    </button>
    <RichFrontmatterInsertForm
      v-if="showInsertChrome && insertOpen"
      :insert-open="insertOpen"
      :draft-key="draftKey"
      :draft-value="draftValue"
      :wiki-titles="wikiTitles"
      :note-id="noteId"
      :property-rows="propertyRows"
      :insert-key-input-id="insertKeyInputId"
      :insert-key-preset-list-id="insertKeyPresetListId"
      @update:draft-key="draftKey = $event"
      @update:draft-value="draftValue = $event"
      @value-blur="tryCommitInsert"
      @dead-link-click="emits('deadLinkClick', $event)"
      @wikidata-dialog-open="openWikidataDialog({ type: 'insert' })"
      @image-upload-state="emits('image-upload-state', $event)"
    />
  </section>
  <WikidataAssociationDialog
    v-if="wikidataDialogOpen"
    ref="wikidataAssociationDialogRef"
    :search-key="wikidataSearchKeyForDialog"
    :model-value="wikidataDialogModelValue"
    :saved-value="wikidataSavedSnapshot"
    :error-message="wikidataIdError"
    :show-save-button="true"
    :can-save-empty-to-clear="wikidataDialogCanSaveEmptyToClear"
    :disabled="wikidataProcessing || isInteractionLocked"
    @close="closeWikidataDialog"
    @save="handleWikidataSave"
    @selected="handleWikidataSelected"
  />
</template>

<script setup lang="ts">
import { Plus } from "@lucide/vue"
import { computed, nextTick, provide, ref, useId, watch } from "vue"
import RichFrontmatterReadOnlyList from "@/components/form/RichFrontmatterReadOnlyList.vue"
import RichFrontmatterEditablePropertyRow from "@/components/form/RichFrontmatterEditablePropertyRow.vue"
import RichFrontmatterInsertForm from "@/components/form/RichFrontmatterInsertForm.vue"
import { richFrontmatterIsReadmeContextKey } from "@/components/form/richFrontmatterProvide"
import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue"
import type { WikiTitle } from "@generated/doughnut-backend-api"
import { usePropertyMemoryTrackerGuard } from "@/composables/usePropertyMemoryTrackerGuard"
import { useWikidataPropertyDialog } from "@/composables/useWikidataPropertyDialog"
import { relationKebabFromLabel } from "@/models/relationTypeOptions"
import { primeSoftKeyboard } from "@/utils/focusTarget"
import {
  findPropertyRowIndexByExactKey,
  isListCapablePropertyKey,
  isRelationPropertyKey,
  isReservedReadmeOnlyPropertyKey,
  normalizePropertyRowForCommit,
  notePropertiesFromPropertyRows,
  parseNoteContentMarkdown,
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
import type { DeadLinkPayload } from "@/utils/wikiPropertyValueField"

const props = defineProps<{
  contentMarkdown: string
  /** When true, properties list is display-only and insert chrome is hidden. */
  readOnly?: boolean
  wikiTitles: WikiTitle[]
  /** Note title for Wikidata search / title comparison when editing `wikidata_id`. */
  noteTitleForWikidataSearch?: string
  /** When set, Wikidata title replace/append updates the note title via the content API. */
  noteId?: number
  /** When true, properties UI is non-interactive (e.g. during image upload). */
  interactionLocked?: boolean
  /** When true, insert/key presets include readme-only keys (`title_pattern`, `question_generation_instruction`). */
  isReadmeContext?: boolean
}>()

const emits = defineEmits<{
  "properties-changed": [rows: PropertyRow[]]
  deadLinkClick: [payload: DeadLinkPayload]
  "image-upload-state": [inProgress: boolean]
}>()

const { confirmAndApplyRemoval, confirmAndApplyRename } =
  usePropertyMemoryTrackerGuard(() => props.noteId)

const isInteractionLocked = computed(() => props.interactionLocked ?? false)

const headingId = useId()
const insertKeyInputId = `${headingId}-insert-key`
const insertKeyPresetListId = `${headingId}-insert-key-presets`

const isReadOnly = computed(() => props.readOnly ?? false)

const indexContextForProvide = computed(() => props.isReadmeContext ?? false)
provide(richFrontmatterIsReadmeContextKey, indexContextForProvide)

const parsed = computed(() => parseNoteContentMarkdown(props.contentMarkdown))

const propertyRows = ref<PropertyRow[]>([])

const insertOpen = ref(false)
const draftKey = ref("")
const draftValue = ref("")

const validationMessage = ref("")
const rowSnapshots = ref<Record<number, PropertyRow>>({})

const wikidataSearchKeyForDialog = computed(
  () => props.noteTitleForWikidataSearch ?? ""
)

const wikidataAssociationDialogRef = ref<InstanceType<
  typeof WikidataAssociationDialog
> | null>(null)

function rowsAfterAdding(row: PropertyRow): PropertyRow[] {
  const properties = notePropertiesFromPropertyRows(propertyRows.value)
  properties[row.key] = row.value
  return sortedPropertyRowsFromNoteProperties(properties)
}

const {
  wikidataDialogOpen,
  wikidataIdError,
  wikidataProcessing,
  wikidataSavedSnapshot,
  wikidataDialogModelValue,
  wikidataDialogCanSaveEmptyToClear,
  openWikidataDialog,
  closeWikidataDialog,
  resetDialog,
  handleWikidataSave,
  handleWikidataSelected,
} = useWikidataPropertyDialog({
  propertyRows,
  draftKey,
  draftValue,
  searchKey: wikidataSearchKeyForDialog,
  noteId: () => props.noteId,
  contentMarkdown: () => props.contentMarkdown,
  rowsAfterAdding,
  onValidationError: (msg) => {
    validationMessage.value = msg
  },
  clearValidation: () => {
    validationMessage.value = ""
  },
  onPropertiesChanged: (rows) =>
    emits("properties-changed", filterForEmit(rows)),
  wikidataAssociationDialogRef,
})

function rowKeyInputId(idx: number) {
  return `${headingId}-row-${idx}-key`
}

function rowKeyPresetListId(idx: number) {
  return `${headingId}-row-${idx}-key-presets`
}

function buildPropertyRows(): PropertyRow[] {
  const p = parsed.value
  if (!p.ok) return []
  return sortedPropertyRowsFromNoteProperties(p.properties)
}

function filterForEmit(rows: PropertyRow[]): PropertyRow[] {
  if (!props.isReadmeContext) return rows
  return rows.filter(
    (r) =>
      !(
        isReservedReadmeOnlyPropertyKey(r.key) &&
        !scalarStringFromPropertyRow(r)?.trim()
      )
  )
}

watch(
  () => props.contentMarkdown,
  () => {
    const p = parsed.value
    if (p.ok) {
      propertyRows.value = buildPropertyRows()
    } else {
      propertyRows.value = []
    }
    insertOpen.value = false
    draftKey.value = ""
    draftValue.value = ""
    validationMessage.value = ""
    rowSnapshots.value = {}
    resetDialog()
  },
  { immediate: true }
)

const headingVisible = computed(
  () => propertyRows.value.length > 0 || isReadOnly.value
)

const showSection = computed(() => {
  if (!parsed.value.ok) return false
  if (isReadOnly.value) return propertyRows.value.length > 0
  return true
})

const showInsertChrome = computed(() => !isReadOnly.value && parsed.value.ok)

async function openPropertyInsert() {
  primeSoftKeyboard()
  insertOpen.value = true
  await nextTick()
  requestAnimationFrame(() => {
    document.getElementById(insertKeyInputId)?.focus()
  })
}

function tryCommitInsert() {
  const key = draftKey.value.trim()
  const value = draftValue.value.trim()
  if (!key || !value) return

  let nextRows: PropertyRow[]
  if (findPropertyRowIndexByExactKey(propertyRows.value, key) >= 0) {
    if (!isListCapablePropertyKey(key)) {
      validationMessage.value = "Duplicate property keys are not allowed."
      return
    }
    nextRows = propertyRowsAfterAppendingValueToExactKey(
      propertyRows.value,
      key,
      value
    )!
  } else {
    nextRows = rowsAfterAdding(propertyRowForInsertedKey(key, value))
  }

  const result = validatePropertyRowsForRichEdit(nextRows)
  if (!result.ok) {
    validationMessage.value = result.message
    return
  }

  validationMessage.value = ""
  emits("properties-changed", filterForEmit(nextRows))
}

function onRowFocus(idx: number) {
  const row = propertyRows.value[idx]
  if (row) {
    rowSnapshots.value[idx] = { ...row }
  }
}

async function removeRow(idx: number) {
  const key = propertyRows.value[idx]?.key.trim() ?? ""
  const proceed = await confirmAndApplyRemoval(key)
  if (!proceed) {
    return
  }

  propertyRows.value = removePropertyRowAt(propertyRows.value, idx)
  validationMessage.value = ""
  emits("properties-changed", filterForEmit([...propertyRows.value]))
}

async function commitRow(idx: number) {
  const snapshot = rowSnapshots.value[idx]
  const rows = propertyRows.value.map((r, i) =>
    i === idx ? normalizePropertyRowForCommit(r) : r
  )
  propertyRows.value = rows

  const result = validatePropertyRowsForRichEdit(propertyRows.value)
  if (!result.ok) {
    validationMessage.value = result.message
    if (snapshot) {
      propertyRows.value = propertyRows.value.map((r, i) =>
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
        propertyRows.value = propertyRows.value.map((r, i) =>
          i === idx ? { ...snapshot } : r
        )
      }
      return
    }
  }

  validationMessage.value = ""
  emits("properties-changed", filterForEmit([...propertyRows.value]))
}

function onRelationTypeSelected(idx: number, newType: string | undefined) {
  if (newType === undefined) return
  const row = propertyRows.value[idx]
  if (!row || !isRelationPropertyKey(row.key)) return
  const current = scalarStringFromPropertyRow(row) ?? ""
  const nextKebab = relationKebabFromLabel(newType)
  if (current.trim().toLowerCase() === nextKebab.toLowerCase()) return
  const rows = propertyRows.value.map((r, i) =>
    i === idx
      ? normalizePropertyRowForCommit({
          ...r,
          value: scalarPropertyValue(nextKebab),
        })
      : normalizePropertyRowForCommit(r)
  )
  propertyRows.value = rows
  const result = validatePropertyRowsForRichEdit(propertyRows.value)
  if (!result.ok) {
    validationMessage.value = result.message
    return
  }
  validationMessage.value = ""
  emits("properties-changed", filterForEmit([...propertyRows.value]))
}

async function addWikiLinkProperty(wikiLinkText: string) {
  const trimmedLink = wikiLinkText.trim()
  const newRows = [
    ...propertyRows.value,
    propertyRowWithScalar("", wikiLinkText),
  ]
  const result = validatePropertyRowsForRichEdit(newRows)
  if (!result.ok) {
    validationMessage.value = result.message
    return
  }
  validationMessage.value = ""
  propertyRows.value = newRows
  emits("properties-changed", filterForEmit([...newRows]))
  await nextTick()
  const idx = propertyRows.value.findIndex(
    (r) =>
      !r.key.trim() && scalarStringFromPropertyRow(r)?.trim() === trimmedLink
  )
  const rowIndex = idx >= 0 ? idx : propertyRows.value.length - 1
  requestAnimationFrame(() => {
    const el = document.querySelector(
      `[data-testid="rich-note-property-row"][data-row-index="${rowIndex}"] [data-testid="rich-note-property-row-key-input"]`
    ) as HTMLInputElement | null
    el?.focus()
  })
}

defineExpose({
  getPropertyRows: (): PropertyRow[] => filterForEmit(propertyRows.value),
  addWikiLinkProperty,
})
</script>

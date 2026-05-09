<template>
  <section
    v-if="showSection"
    class="daisy-mb-3"
    :class="
      isInteractionLocked
        ? 'daisy-pointer-events-none daisy-opacity-60'
        : ''
    "
    :aria-labelledby="headingVisible ? headingId : undefined"
    :aria-label="headingVisible ? undefined : 'Note properties'"
  >
    <div
      v-if="headingVisible"
      class="daisy-flex daisy-items-center daisy-justify-between daisy-gap-2 daisy-mb-2"
    >
      <h4
        :id="headingId"
        class="daisy-mb-0 daisy-text-sm daisy-font-semibold"
      >
        Properties
      </h4>
      <button
        v-if="showInsertChrome && propertyRows.length > 0"
        type="button"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-inline-flex daisy-shrink-0 daisy-items-center daisy-gap-1"
        @click="openPropertyInsert"
      >
        <Plus class="daisy-h-4 daisy-w-4" aria-hidden="true" />
        Add property
      </button>
    </div>
    <RichFrontmatterReadOnlyList
      v-if="propertyRows.length > 0 && isReadOnly"
      :property-rows="propertyRows"
    />
    <div
      v-else-if="propertyRows.length > 0"
      class="daisy-flex daisy-flex-col daisy-gap-2 daisy-text-sm"
    >
      <RichFrontmatterEditablePropertyRow
        v-for="(_, idx) in propertyRows"
        :key="idx"
        v-model="propertyRows[idx]!"
        :idx="idx"
        :wiki-titles="wikiTitles"
        :note-id="noteId"
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
      class="daisy-text-error daisy-text-xs daisy-mt-1"
      data-testid="rich-note-property-validation"
    >
      {{ validationMessage }}
    </p>
    <button
      v-if="showInsertChrome && !insertOpen && propertyRows.length === 0"
      type="button"
      class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-inline-flex daisy-self-start daisy-items-center daisy-gap-1"
      @click="openPropertyInsert"
    >
      <Plus class="daisy-h-4 daisy-w-4" aria-hidden="true" />
      Add property
    </button>
    <RichFrontmatterInsertForm
      v-if="showInsertChrome && insertOpen"
      :insert-open="insertOpen"
      :draft-key="draftKey"
      :draft-value="draftValue"
      :wiki-titles="wikiTitles"
      :note-id="noteId"
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
import { Plus } from "lucide-vue-next"
import { computed, nextTick, ref, useId, watch } from "vue"
import RichFrontmatterReadOnlyList from "@/components/form/RichFrontmatterReadOnlyList.vue"
import RichFrontmatterEditablePropertyRow from "@/components/form/RichFrontmatterEditablePropertyRow.vue"
import RichFrontmatterInsertForm from "@/components/form/RichFrontmatterInsertForm.vue"
import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue"
import type { WikiTitle } from "@generated/doughnut-backend-api"
import { useWikidataPropertyDialog } from "@/composables/useWikidataPropertyDialog"
import { relationKebabFromLabel } from "@/models/relationTypeOptions"
import {
  INDEX_ONLY_PRESET_PROPERTY_KEYS,
  isRelationPropertyKey,
  parseNoteContentMarkdown,
  removePropertyRowAt,
  sortedPropertyRowsFromRecord,
  validatePropertyRowsForRichEdit,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"

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
  /** When true, shows index-only predefined property rows (titlePattern, questionGenerationInstruction). */
  isIndexContext?: boolean
}>()

const emits = defineEmits<{
  "properties-changed": [rows: PropertyRow[]]
  deadLinkClick: [title: string]
  "image-upload-state": [inProgress: boolean]
}>()

const isInteractionLocked = computed(() => props.interactionLocked ?? false)

const headingId = useId()
const insertKeyInputId = `${headingId}-insert-key`
const insertKeyPresetListId = `${headingId}-insert-key-presets`

const isReadOnly = computed(() => props.readOnly ?? false)

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
  const record = Object.fromEntries(
    propertyRows.value.map((r) => [r.key, r.value])
  )
  record[row.key] = row.value
  return sortedPropertyRowsFromRecord(record)
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
  let rows = sortedPropertyRowsFromRecord(p.properties)
  if (props.isIndexContext && !isReadOnly.value) {
    for (const key of INDEX_ONLY_PRESET_PROPERTY_KEYS) {
      if (!rows.some((r) => r.key === key)) {
        rows = [...rows, { key, value: "" }]
      }
    }
  }
  return rows
}

function filterForEmit(rows: PropertyRow[]): PropertyRow[] {
  if (!props.isIndexContext) return rows
  return rows.filter(
    (r) =>
      !(
        (INDEX_ONLY_PRESET_PROPERTY_KEYS as readonly string[]).includes(
          r.key
        ) && !r.value.trim()
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

  if (propertyRows.value.some((r) => r.key.trim() === key)) {
    validationMessage.value = "Duplicate property keys are not allowed."
    return
  }

  const nextRows = rowsAfterAdding({ key, value })
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

function removeRow(idx: number) {
  propertyRows.value = removePropertyRowAt(propertyRows.value, idx)
  validationMessage.value = ""
  emits("properties-changed", filterForEmit([...propertyRows.value]))
}

function commitRow(idx: number) {
  const snapshot = rowSnapshots.value[idx]
  const rows = propertyRows.value.map((r, i) =>
    i === idx ? { key: r.key.trim(), value: r.value.trim() } : r
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

  validationMessage.value = ""
  emits("properties-changed", filterForEmit([...propertyRows.value]))
}

function onRelationTypeSelected(idx: number, newType: string | undefined) {
  if (newType === undefined) return
  const row = propertyRows.value[idx]
  if (!row || !isRelationPropertyKey(row.key)) return
  const nextKebab = relationKebabFromLabel(newType)
  if (row.value.trim().toLowerCase() === nextKebab.toLowerCase()) return
  const rows = propertyRows.value.map((r, i) =>
    i === idx
      ? { key: r.key.trim(), value: nextKebab }
      : { key: r.key.trim(), value: r.value.trim() }
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
  const newRows = [...propertyRows.value, { key: "", value: wikiLinkText }]
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
    (r) => !r.key.trim() && r.value.trim() === trimmedLink
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

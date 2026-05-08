<template>
  <section
    v-if="showSection"
    class="daisy-mb-3"
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
        :key-input-id="rowKeyInputId(idx)"
        :preset-list-id="rowKeyPresetListId(idx)"
        @row-focus="onRowFocus(idx)"
        @commit="commitRow(idx)"
        @remove="removeRow(idx)"
        @wikidata-dialog-open="openWikidataDialog({ type: 'row', idx })"
        @dead-link-click="emits('deadLinkClick', $event)"
        @relation-type-selected="onRelationTypeSelected(idx, $event)"
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
    <RichFrontmatterInsertForm
      v-if="showInsertChrome && (propertyRows.length === 0 || insertOpen)"
      :insert-open="insertOpen"
      :show-insert-button="propertyRows.length === 0"
      :draft-key="draftKey"
      :draft-value="draftValue"
      :wiki-titles="wikiTitles"
      :insert-key-input-id="insertKeyInputId"
      :insert-key-preset-list-id="insertKeyPresetListId"
      @open-insert="openPropertyInsert"
      @update:draft-key="draftKey = $event"
      @update:draft-value="draftValue = $event"
      @value-blur="tryCommitInsert"
      @dead-link-click="emits('deadLinkClick', $event)"
      @wikidata-dialog-open="openWikidataDialog({ type: 'insert' })"
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
    :disabled="wikidataProcessing"
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
}>()

const emits = defineEmits<{
  "properties-changed": [rows: PropertyRow[]]
  deadLinkClick: [title: string]
}>()

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
  onPropertiesChanged: (rows) => emits("properties-changed", rows),
  wikidataAssociationDialogRef,
})

function rowKeyInputId(idx: number) {
  return `${headingId}-row-${idx}-key`
}

function rowKeyPresetListId(idx: number) {
  return `${headingId}-row-${idx}-key-presets`
}

watch(
  () => props.contentMarkdown,
  () => {
    const p = parsed.value
    if (p.ok) {
      propertyRows.value = sortedPropertyRowsFromRecord(p.properties)
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
  emits("properties-changed", nextRows)
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
  emits("properties-changed", [...propertyRows.value])
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
  emits("properties-changed", [...propertyRows.value])
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
  emits("properties-changed", [...propertyRows.value])
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
  emits("properties-changed", [...newRows])
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
  getPropertyRows: (): PropertyRow[] => propertyRows.value,
  addWikiLinkProperty,
})
</script>

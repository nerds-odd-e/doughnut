<template>
  <section
    v-if="showSection"
    class="daisy-mb-3"
    :aria-labelledby="headingVisible ? headingId : undefined"
    :aria-label="headingVisible ? undefined : 'Note properties'"
  >
    <h4
      v-if="headingVisible"
      :id="headingId"
      class="daisy-text-sm daisy-font-semibold daisy-mb-2"
    >
      Properties
    </h4>
    <dl
      v-if="propertyRows.length > 0 && isReadOnly"
      class="daisy-grid daisy-grid-cols-[auto_minmax(0,1fr)] daisy-gap-x-4 daisy-gap-y-1 daisy-text-sm"
    >
      <template v-for="row in propertyRows" :key="row.key">
        <dt class="daisy-font-medium daisy-text-base-content/80">{{ row.key }}</dt>
        <dd class="daisy-m-0">
          {{ isRelationPropertyRow(row) ? relationLabelFromKebab(row.value) : row.value }}
        </dd>
      </template>
    </dl>
    <div
      v-else-if="propertyRows.length > 0"
      class="daisy-flex daisy-flex-col daisy-gap-2 daisy-text-sm"
    >
      <div
        v-for="(row, idx) in propertyRows"
        :key="idx"
        class="daisy-grid daisy-grid-cols-[minmax(8rem,auto)_minmax(0,1fr)_auto] daisy-gap-x-4 daisy-gap-y-1 daisy-items-center"
        data-testid="rich-note-property-row"
        :data-property-key="row.key"
      >
        <input
          v-model="propertyRows[idx]!.key"
          type="text"
          autocapitalize="off"
          class="daisy-input daisy-input-bordered daisy-input-sm daisy-w-full daisy-min-w-[8rem]"
          :aria-label="`Existing note property key (row ${idx + 1})`"
          data-testid="rich-note-property-row-key-input"
          @focus="onRowFocus(idx)"
          @blur="commitRow(idx)"
        >
        <RelationTypeSelectCompact
          v-if="isRelationPropertyRow(propertyRows[idx]!)"
          field="relationType"
          scope-name="rich-note-relation-property"
          hide-label
          :model-value="relationTypeFromKebab(propertyRows[idx]!.value)"
          :inverse-icon="true"
          @update:model-value="onRelationTypeSelected(idx, $event)"
        />
        <input
          v-else
          v-model="propertyRows[idx]!.value"
          type="text"
          class="daisy-input daisy-input-bordered daisy-input-sm daisy-w-full"
          :aria-label="`Existing note property value (row ${idx + 1})`"
          data-testid="rich-note-property-row-value-input"
          @focus="onRowFocus(idx)"
          @blur="commitRow(idx)"
        >
        <button
          type="button"
          class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-shrink-0"
          :aria-label="`Remove note property ${row.key}`"
          data-testid="rich-note-property-row-remove"
          @click="removeRow(idx)"
        >
          Remove
        </button>
      </div>
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
    <div v-if="showInsertChrome" class="daisy-flex daisy-flex-col daisy-gap-2 daisy-mt-1">
      <button
        type="button"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-self-start"
        @click="insertOpen = true"
      >
        Add note property
      </button>
      <div v-if="insertOpen" class="daisy-flex daisy-flex-wrap daisy-gap-2 daisy-items-end">
        <label class="daisy-form-control daisy-w-full sm:daisy-w-auto daisy-min-w-[8rem]">
          <span class="daisy-label daisy-text-xs">Property key</span>
          <input
            v-model="draftKey"
            type="text"
            autocapitalize="off"
            class="daisy-input daisy-input-bordered daisy-input-sm daisy-w-full"
            aria-label="Property key"
            data-testid="rich-note-property-key"
            @keydown.enter.prevent="focusValueInput"
          >
        </label>
        <label class="daisy-form-control daisy-w-full sm:daisy-flex-1 daisy-min-w-[8rem]">
          <span class="daisy-label daisy-text-xs">Property value</span>
          <input
            ref="valueInputRef"
            v-model="draftValue"
            type="text"
            class="daisy-input daisy-input-bordered daisy-input-sm daisy-w-full"
            aria-label="Property value"
            data-testid="rich-note-property-value"
            @keydown.enter.prevent="tryCommitInsert"
            @blur="tryCommitInsert"
          >
        </label>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, useId, watch } from "vue"
import RelationTypeSelectCompact from "@/components/links/RelationTypeSelectCompact.vue"
import {
  relationKebabFromLabel,
  relationLabelFromKebab,
  relationTypeFromKebab,
  type RelationTypeLabel,
} from "@/models/relationTypeOptions"
import {
  parseNoteDetailsMarkdown,
  removePropertyRowAt,
  sortedPropertyRowsFromRecord,
  validatePropertyRowsForRichEdit,
  type PropertyRow,
} from "@/utils/noteDetailsFrontmatter"

const props = defineProps<{
  detailsMarkdown: string
  /** When true, properties list is display-only and insert chrome is hidden. */
  readOnly?: boolean
}>()

const emits = defineEmits<{
  "properties-changed": [rows: PropertyRow[]]
}>()

const headingId = useId()

const isReadOnly = computed(() => props.readOnly ?? false)

const parsed = computed(() => parseNoteDetailsMarkdown(props.detailsMarkdown))

const propertyRows = ref<PropertyRow[]>([])

const insertOpen = ref(false)
const draftKey = ref("")
const draftValue = ref("")
const valueInputRef = ref<HTMLInputElement | null>(null)

const validationMessage = ref("")
const rowSnapshots = ref<Record<number, PropertyRow>>({})

watch(
  () => props.detailsMarkdown,
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
  },
  { immediate: true }
)

function isRelationPropertyRow(row: PropertyRow): boolean {
  return row.key.trim().toLowerCase() === "relation"
}

function onRelationTypeSelected(
  idx: number,
  newType: RelationTypeLabel | undefined
) {
  if (newType === undefined) return
  const row = propertyRows.value[idx]
  if (!row || !isRelationPropertyRow(row)) return
  const current = relationTypeFromKebab(row.value)
  if (current === newType) return
  const kebab = relationKebabFromLabel(newType)
  const rows = propertyRows.value.map((r, i) =>
    i === idx
      ? { key: r.key.trim(), value: kebab }
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

const headingVisible = computed(
  () => propertyRows.value.length > 0 || isReadOnly.value
)

const showSection = computed(() => {
  if (!parsed.value.ok) return false
  if (isReadOnly.value) return propertyRows.value.length > 0
  return true
})

const showInsertChrome = computed(() => !isReadOnly.value && parsed.value.ok)

function focusValueInput() {
  valueInputRef.value?.focus()
}

function rowsAfterAdding(row: PropertyRow): PropertyRow[] {
  const record = Object.fromEntries(
    propertyRows.value.map((r) => [r.key, r.value])
  )
  record[row.key] = row.value
  return sortedPropertyRowsFromRecord(record)
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

defineExpose({
  getPropertyRows: (): PropertyRow[] => propertyRows.value,
})
</script>

<template>
  <section
    v-if="showSection"
    class="daisy-mb-3"
    :aria-labelledby="headingId"
  >
    <h4
      :id="headingId"
      class="daisy-text-sm daisy-font-semibold daisy-mb-2"
    >
      Properties
    </h4>
    <dl
      v-if="propertyRows.length > 0"
      class="daisy-grid daisy-grid-cols-[auto_minmax(0,1fr)] daisy-gap-x-4 daisy-gap-y-1 daisy-text-sm"
    >
      <template v-for="row in propertyRows" :key="row.key">
        <dt class="daisy-font-medium daisy-text-base-content/80">{{ row.key }}</dt>
        <dd class="daisy-m-0">{{ row.value }}</dd>
      </template>
    </dl>
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
import {
  parseNoteDetailsMarkdown,
  sortedPropertyRowsFromRecord,
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
  },
  { immediate: true }
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

  const nextRows = rowsAfterAdding({ key, value })
  emits("properties-changed", nextRows)
}

defineExpose({
  getPropertyRows: (): PropertyRow[] => propertyRows.value,
})
</script>

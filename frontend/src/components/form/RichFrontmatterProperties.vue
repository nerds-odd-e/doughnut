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
    <dl
      v-if="propertyRows.length > 0 && isReadOnly"
      class="daisy-grid daisy-grid-cols-[auto_minmax(0,1fr)] daisy-gap-x-4 daisy-gap-y-1 daisy-text-sm"
    >
      <template v-for="row in propertyRows" :key="row.key">
        <dt class="daisy-font-medium daisy-text-base-content/80">{{ row.key }}</dt>
        <dd class="daisy-m-0">
          {{
            isRelationPropertyRow(row)
              ? relationLabelFromKebab(row.value)
              : isWikidataIdPropertyRow(row)
                ? row.value.trim() || "—"
                : row.value
          }}
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
        :data-row-index="idx"
        :data-property-key="row.key"
      >
        <div
          class="daisy-relative daisy-min-w-[8rem]"
          @focusout="onKeyPresetWrapperFocusOut($event, 'row', idx)"
        >
          <input
            :id="rowKeyInputId(idx)"
            v-model="propertyRows[idx]!.key"
            type="text"
            autocapitalize="off"
            class="daisy-input daisy-input-bordered daisy-input-sm daisy-w-full daisy-min-w-[8rem]"
            :aria-label="`Existing note property key (row ${idx + 1})`"
            :aria-expanded="presetPanelOpenForRow(idx)"
            :aria-controls="
              presetPanelOpenForRow(idx) ? rowKeyPresetListId(idx) : undefined
            "
            data-testid="rich-note-property-row-key-input"
            @focus="onRowKeyInputFocus(idx)"
            @blur="commitRow(idx)"
          >
          <ul
            v-if="presetPanelOpenForRow(idx)"
            :id="rowKeyPresetListId(idx)"
            role="listbox"
            class="daisy-menu daisy-absolute daisy-left-0 daisy-right-0 daisy-top-full daisy-z-20 daisy-mt-0.5 daisy-w-full daisy-rounded-box daisy-bg-base-100 daisy-p-1 daisy-shadow"
            data-testid="rich-note-property-key-preset-list"
          >
            <li
              v-for="presetKey in RICH_MODE_PRESET_PROPERTY_KEYS"
              :key="presetKey"
            >
              <button
                type="button"
                role="option"
                class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-w-full daisy-justify-start daisy-font-mono"
                data-testid="rich-note-property-key-preset-option"
                :data-preset-key="presetKey"
                @mousedown.prevent
                @click="selectPresetForRow(idx, presetKey)"
              >
                {{ presetKey }}
              </button>
            </li>
          </ul>
        </div>
        <RelationTypeSelectCompact
          v-if="isRelationPropertyRow(propertyRows[idx]!)"
          field="relationType"
          scope-name="rich-note-relation-property"
          hide-label
          :model-value="relationModelValue(propertyRows[idx]!)"
          :inverse-icon="true"
          @update:model-value="onRelationTypeSelected(idx, $event)"
        />
        <div
          v-else-if="isWikidataIdPropertyRow(propertyRows[idx]!)"
          class="daisy-flex daisy-min-w-0 daisy-items-center daisy-gap-2"
          :class="
            propertyRows[idx]!.value.trim()
              ? ''
              : 'daisy-justify-between'
          "
        >
          <button
            v-if="propertyRows[idx]!.value.trim()"
            type="button"
            class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-h-auto daisy-min-h-0 daisy-min-w-0 daisy-max-w-full daisy-shrink daisy-truncate daisy-justify-start daisy-py-0.5 daisy-px-1 daisy-font-mono daisy-text-sm daisy-font-normal daisy-text-base-content/90 daisy-normal-case"
            :title="propertyRows[idx]!.value.trim()"
            data-testid="rich-note-wikidata-property-edit"
            :aria-label="`Edit Wikidata ID ${propertyRows[idx]!.value.trim()}`"
            @click="openWikidataDialogForRow(idx)"
          >
            {{ propertyRows[idx]!.value.trim() }}
          </button>
          <template v-else>
            <span
              class="daisy-truncate daisy-font-mono daisy-text-sm daisy-text-base-content/90"
              aria-hidden="true"
            >—</span>
            <button
              type="button"
              class="daisy-btn daisy-btn-sm daisy-btn-outline daisy-shrink-0"
              data-testid="rich-note-wikidata-property-edit"
              aria-label="Set Wikidata ID"
              @click="openWikidataDialogForRow(idx)"
            >
              Set…
            </button>
          </template>
        </div>
        <WikiPropertyValueField
          v-else
          v-model="propertyRows[idx]!.value"
          :wiki-titles="wikiTitles"
          :aria-label="`Existing note property value (row ${idx + 1})`"
          data-testid="rich-note-property-row-value-input"
          @focus="onRowFocus(idx)"
          @blur="commitRow(idx)"
          @dead-link-click="emits('deadLinkClick', $event)"
        />
        <button
          type="button"
          class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-square daisy-shrink-0"
          :aria-label="`Remove note property ${row.key}`"
          data-testid="rich-note-property-row-remove"
          @click="removeRow(idx)"
        >
          <Minus class="daisy-h-4 daisy-w-4" aria-hidden="true" />
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
    <div
      v-if="showInsertChrome && (propertyRows.length === 0 || insertOpen)"
      :class="
        propertyRows.length === 0
          ? 'daisy-mt-1 daisy-flex daisy-flex-col daisy-gap-2'
          : 'daisy-mt-1'
      "
    >
      <button
        v-if="propertyRows.length === 0"
        type="button"
        class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-inline-flex daisy-self-start daisy-items-center daisy-gap-1"
        @click="openPropertyInsert"
      >
        <Plus class="daisy-h-4 daisy-w-4" aria-hidden="true" />
        Add property
      </button>
      <div
        v-if="insertOpen"
        class="daisy-flex daisy-flex-wrap daisy-gap-2 daisy-items-end"
      >
        <label class="daisy-form-control daisy-w-full sm:daisy-w-auto daisy-min-w-[8rem]">
          <span class="daisy-label daisy-text-xs">Property key</span>
          <div
            class="daisy-relative daisy-w-full"
            @focusout="onKeyPresetWrapperFocusOut($event, 'insert')"
          >
            <input
              :id="insertKeyInputId"
              v-model="draftKey"
              type="text"
              autocapitalize="off"
              class="daisy-input daisy-input-bordered daisy-input-sm daisy-w-full"
              aria-label="Property key"
              :aria-expanded="presetPanelOpenForInsert"
              :aria-controls="
                presetPanelOpenForInsert ? insertKeyPresetListId : undefined
              "
              data-testid="rich-note-property-key"
              @focus="onInsertKeyInputFocus"
              @keydown.enter.prevent="focusValueInput"
            >
            <ul
              v-if="presetPanelOpenForInsert"
              :id="insertKeyPresetListId"
              role="listbox"
              class="daisy-menu daisy-absolute daisy-left-0 daisy-right-0 daisy-top-full daisy-z-20 daisy-mt-0.5 daisy-w-full daisy-rounded-box daisy-bg-base-100 daisy-p-1 daisy-shadow"
              data-testid="rich-note-property-key-preset-list"
            >
              <li
                v-for="presetKey in RICH_MODE_PRESET_PROPERTY_KEYS"
                :key="presetKey"
              >
                <button
                  type="button"
                  role="option"
                  class="daisy-btn daisy-btn-ghost daisy-btn-sm daisy-w-full daisy-justify-start daisy-font-mono"
                  data-testid="rich-note-property-key-preset-option"
                  :data-preset-key="presetKey"
                  @mousedown.prevent
                  @click="selectPresetForInsert(presetKey)"
                >
                  {{ presetKey }}
                </button>
              </li>
            </ul>
          </div>
        </label>
        <label class="daisy-form-control daisy-w-full sm:daisy-flex-1 daisy-min-w-[8rem]">
          <span class="daisy-label daisy-text-xs">Property value</span>
          <div
            v-if="isWikidataIdPropertyKey(draftKey)"
            class="daisy-flex daisy-flex-wrap daisy-items-center daisy-gap-2"
          >
            <span class="daisy-font-mono daisy-text-sm">{{ draftValue.trim() || "—" }}</span>
            <button
              type="button"
              class="daisy-btn daisy-btn-sm daisy-btn-outline"
              data-testid="rich-note-wikidata-property-insert-edit"
              @click="openWikidataDialogForInsert"
            >
              Set…
            </button>
          </div>
          <WikiPropertyValueField
            v-else
            ref="valueInputRef"
            v-model="draftValue"
            :wiki-titles="wikiTitles"
            aria-label="Property value"
            data-testid="rich-note-property-value"
            @blur="tryCommitInsert"
            @dead-link-click="emits('deadLinkClick', $event)"
          />
        </label>
      </div>
    </div>
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
import { Minus, Plus } from "lucide-vue-next"
import { computed, nextTick, ref, useId, watch } from "vue"
import WikiPropertyValueField from "@/components/form/WikiPropertyValueField.vue"
import RelationTypeSelectCompact from "@/components/links/RelationTypeSelectCompact.vue"
import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue"
import type {
  WikiTitle,
  WikidataSearchEntity,
} from "@generated/doughnut-backend-api"
import { WikidataController } from "@generated/doughnut-backend-api/sdk.gen"
import {} from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"
import { calculateNewTitle } from "@/utils/wikidataTitleActions"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import {
  isKnownRelationKebab,
  relationKebabFromLabel,
  relationLabelFromKebab,
  relationTypeFromKebab,
} from "@/models/relationTypeOptions"
import {
  isWikidataIdPropertyKey,
  parseNoteContentMarkdown,
  RICH_MODE_PRESET_PROPERTY_KEYS,
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

const storageAccessor = useStorageAccessor()

const emits = defineEmits<{
  "properties-changed": [rows: PropertyRow[]]
  deadLinkClick: [title: string]
}>()

const headingId = useId()
const insertKeyInputId = `${headingId}-insert-key`
const insertKeyPresetListId = `${headingId}-insert-key-presets`

const isReadOnly = computed(() => props.readOnly ?? false)

type PresetPanelTarget = { kind: "row"; idx: number } | { kind: "insert" }

const presetPanel = ref<PresetPanelTarget | null>(null)

const presetPanelOpenForInsert = computed(
  () => presetPanel.value?.kind === "insert"
)

function rowKeyInputId(idx: number) {
  return `${headingId}-row-${idx}-key`
}

function rowKeyPresetListId(idx: number) {
  return `${headingId}-row-${idx}-key-presets`
}

function presetPanelOpenForRow(idx: number) {
  return presetPanel.value?.kind === "row" && presetPanel.value.idx === idx
}

function onRowKeyInputFocus(idx: number) {
  onRowFocus(idx)
  presetPanel.value = { kind: "row", idx }
}

function onInsertKeyInputFocus() {
  presetPanel.value = { kind: "insert" }
}

function onKeyPresetWrapperFocusOut(
  event: FocusEvent,
  kind: "row" | "insert",
  idx?: number
) {
  const root = event.currentTarget as HTMLElement | null
  const next = event.relatedTarget as Node | null
  if (root && next && root.contains(next)) return
  if (kind === "row" && idx !== undefined) {
    if (presetPanel.value?.kind === "row" && presetPanel.value.idx === idx) {
      presetPanel.value = null
    }
    return
  }
  if (kind === "insert" && presetPanel.value?.kind === "insert") {
    presetPanel.value = null
  }
}

function selectPresetForRow(idx: number, key: string) {
  const row = propertyRows.value[idx]
  if (!row) return
  row.key = key
  presetPanel.value = null
  requestAnimationFrame(() => {
    document.getElementById(rowKeyInputId(idx))?.focus()
  })
}

function selectPresetForInsert(key: string) {
  draftKey.value = key
  presetPanel.value = null
  requestAnimationFrame(() => {
    document.getElementById(insertKeyInputId)?.focus()
  })
}

const parsed = computed(() => parseNoteContentMarkdown(props.contentMarkdown))

const propertyRows = ref<PropertyRow[]>([])

const insertOpen = ref(false)
const draftKey = ref("")
const draftValue = ref("")
const valueInputRef = ref<InstanceType<typeof WikiPropertyValueField> | null>(
  null
)

const validationMessage = ref("")
const rowSnapshots = ref<Record<number, PropertyRow>>({})

type WikidataEditContext = { type: "row"; idx: number } | { type: "insert" }

const wikidataDialogOpen = ref(false)
const wikidataEditContext = ref<WikidataEditContext | null>(null)
const wikidataIdError = ref<string | undefined>()
const wikidataProcessing = ref(false)
const wikidataSavedSnapshot = ref("")
const wikidataAssociationDialogRef = ref<InstanceType<
  typeof WikidataAssociationDialog
> | null>(null)

const wikidataSearchKeyForDialog = computed(
  () => props.noteTitleForWikidataSearch ?? ""
)

const wikidataDialogModelValue = computed(() => {
  const c = wikidataEditContext.value
  if (!c) return ""
  if (c.type === "row") return propertyRows.value[c.idx]?.value ?? ""
  return draftValue.value
})

const wikidataDialogCanSaveEmptyToClear = computed(() => {
  const c = wikidataEditContext.value
  if (!c) return false
  if (c.type === "row") return !!propertyRows.value[c.idx]?.value.trim()
  return !!draftValue.value.trim()
})

const showTitleOptionsInDialog = computed(() => {
  const d = wikidataAssociationDialogRef.value as
    | { showTitleOptions?: boolean }
    | null
    | undefined
  return d?.showTitleOptions ?? false
})

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
    wikidataDialogOpen.value = false
    wikidataEditContext.value = null
    wikidataIdError.value = undefined
    wikidataProcessing.value = false
    presetPanel.value = null
  },
  { immediate: true }
)

function isWikidataIdPropertyRow(row: PropertyRow): boolean {
  return isWikidataIdPropertyKey(row.key)
}

function isRelationPropertyRow(row: PropertyRow): boolean {
  return row.key.trim().toLowerCase() === "relation"
}

function relationModelValue(row: PropertyRow): string {
  if (isKnownRelationKebab(row.value)) return relationTypeFromKebab(row.value)
  return row.value.trim()
}

function onRelationTypeSelected(idx: number, newType: string | undefined) {
  if (newType === undefined) return
  const row = propertyRows.value[idx]
  if (!row || !isRelationPropertyRow(row)) return
  const nextKebab = relationKebabFromLabel(newType)
  if (row.value.trim().toLowerCase() === nextKebab.toLowerCase()) return
  const kebab = nextKebab
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

async function openPropertyInsert() {
  insertOpen.value = true
  await nextTick()
  requestAnimationFrame(() => {
    document.getElementById(insertKeyInputId)?.focus()
  })
}

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

function openWikidataDialogForRow(idx: number) {
  wikidataEditContext.value = { type: "row", idx }
  wikidataSavedSnapshot.value = propertyRows.value[idx]?.value.trim() ?? ""
  wikidataIdError.value = undefined
  wikidataDialogOpen.value = true
}

function openWikidataDialogForInsert() {
  wikidataEditContext.value = { type: "insert" }
  wikidataSavedSnapshot.value = draftValue.value.trim()
  wikidataIdError.value = undefined
  wikidataDialogOpen.value = true
}

function closeWikidataDialog() {
  wikidataDialogOpen.value = false
  wikidataEditContext.value = null
  wikidataIdError.value = undefined
  wikidataProcessing.value = false
}

function applyWikidataIdToRow(idx: number, wikidataId: string) {
  const rows = propertyRows.value.map((r, i) =>
    i === idx
      ? { key: r.key.trim(), value: wikidataId }
      : { key: r.key.trim(), value: r.value.trim() }
  )
  propertyRows.value = rows
  const result = validatePropertyRowsForRichEdit(propertyRows.value)
  if (!result.ok) {
    validationMessage.value = result.message
    wikidataIdError.value = result.message
    return
  }
  validationMessage.value = ""
  wikidataIdError.value = undefined
  emits("properties-changed", [...propertyRows.value])
  closeWikidataDialog()
}

function commitInsertWithWikidataValue(trimmed: string) {
  const key = draftKey.value.trim()
  if (!key || !trimmed) return
  if (propertyRows.value.some((r) => r.key.trim() === key)) {
    wikidataIdError.value = "Duplicate property keys are not allowed."
    return
  }
  const nextRows = rowsAfterAdding({ key, value: trimmed })
  const result = validatePropertyRowsForRichEdit(nextRows)
  if (!result.ok) {
    validationMessage.value = result.message
    wikidataIdError.value = result.message
    return
  }
  validationMessage.value = ""
  wikidataIdError.value = undefined
  emits("properties-changed", nextRows)
  closeWikidataDialog()
}

async function applyWikidataIdAndClose(wikidataId: string) {
  const ctx = wikidataEditContext.value
  if (!ctx) return
  const trimmed = wikidataId.trim()
  if (ctx.type === "row") {
    applyWikidataIdToRow(ctx.idx, trimmed)
    return
  }
  draftValue.value = trimmed
  if (!trimmed) {
    closeWikidataDialog()
    return
  }
  commitInsertWithWikidataValue(trimmed)
}

async function persistWikidataIdViaValidation(wikidataId: string) {
  if (wikidataId.trim() === "") {
    await applyWikidataIdAndClose("")
    wikidataProcessing.value = false
    return
  }
  wikidataProcessing.value = true
  wikidataIdError.value = undefined
  try {
    const { data: entityData, error } =
      await WikidataController.fetchWikidataEntityDataById({
        path: { wikidataId: wikidataId.trim() },
      })
    if (error) {
      wikidataIdError.value =
        toOpenApiError(error).message || "Invalid Wikidata ID"
      wikidataProcessing.value = false
      return
    }
    const noteTitleUpper = wikidataSearchKeyForDialog.value.trim().toUpperCase()
    const wikidataTitleUpper = entityData!.WikidataTitleInEnglish.toUpperCase()
    if (
      wikidataTitleUpper === noteTitleUpper ||
      entityData!.WikidataTitleInEnglish === ""
    ) {
      await applyWikidataIdAndClose(wikidataId)
      wikidataProcessing.value = false
      return
    }
    wikidataProcessing.value = false
    const entity: WikidataSearchEntity = {
      id: wikidataId.trim(),
      label: entityData!.WikidataTitleInEnglish,
      description: "",
    }
    wikidataAssociationDialogRef.value?.showTitleOptionsForEntity(entity)
  } catch (e: unknown) {
    wikidataIdError.value =
      toOpenApiError(e).message || "An unknown error occurred"
    wikidataProcessing.value = false
  }
}

async function handleWikidataSave(wikidataId: string) {
  if (wikidataProcessing.value) return
  if (wikidataId.trim() === "") {
    await persistWikidataIdViaValidation("")
    return
  }
  if (showTitleOptionsInDialog.value) {
    await applyWikidataIdAndClose(wikidataId)
    wikidataProcessing.value = false
    return
  }
  await persistWikidataIdViaValidation(wikidataId)
}

async function handleWikidataSelected(
  entity: WikidataSearchEntity,
  titleAction?: "replace" | "append"
) {
  if (!entity.id || wikidataProcessing.value) return
  wikidataProcessing.value = true
  wikidataIdError.value = undefined
  try {
    if (titleAction && props.noteId != null) {
      const currentTitle = wikidataSearchKeyForDialog.value.trim()
      if (currentTitle) {
        const newTitle = calculateNewTitle(currentTitle, entity, titleAction)
        await storageAccessor.value
          .storedApi()
          .updateTextField(props.noteId, "edit title", newTitle)
      }
    }
    await applyWikidataIdAndClose(entity.id)
  } catch (e: unknown) {
    wikidataIdError.value =
      toOpenApiError(e).message || "An unknown error occurred"
  } finally {
    wikidataProcessing.value = false
  }
}

defineExpose({
  getPropertyRows: (): PropertyRow[] => propertyRows.value,
  addWikiLinkProperty,
})
</script>

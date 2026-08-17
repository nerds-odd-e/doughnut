<template>
  <section
    v-if="showSection"
    class="mb-3"
    :class="isInteractionLocked ? 'pointer-events-none opacity-60' : ''"
    :aria-labelledby="headingVisible ? headingId : undefined"
    :aria-label="headingVisible ? undefined : 'Note properties'"
  >
    <div
      v-if="headingVisible"
      class="flex items-center justify-between gap-2 mb-2"
    >
      <h4 :id="headingId" class="mb-0 text-sm font-semibold">Properties</h4>
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
      :wiki-titles="wikiTitles"
    />
    <div
      v-else-if="propertyRows.length > 0"
      class="flex flex-col gap-2 text-sm"
    >
      <RichFrontmatterEditablePropertyRow
        v-for="(_, idx) in propertyRows"
        :key="rowClientIds[idx]"
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
        @dead-wiki-link-click="emits('deadWikiLinkClick', $event)"
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
      @dead-wiki-link-click="emits('deadWikiLinkClick', $event)"
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
import { computed, provide, ref, useId, watch } from "vue"
import RichFrontmatterReadOnlyList from "@/components/form/RichFrontmatterReadOnlyList.vue"
import RichFrontmatterEditablePropertyRow from "@/components/form/RichFrontmatterEditablePropertyRow.vue"
import RichFrontmatterInsertForm from "@/components/form/RichFrontmatterInsertForm.vue"
import { richFrontmatterIsReadmeContextKey } from "@/components/form/richFrontmatterProvide"
import WikidataAssociationDialog from "@/components/notes/WikidataAssociationDialog.vue"
import type { WikiTitle } from "@generated/doughnut-backend-api"
import { usePropertyRowClientIds } from "@/composables/usePropertyRowClientIds"
import { useRichFrontmatterPropertyEditing } from "@/composables/useRichFrontmatterPropertyEditing"
import { useWikidataPropertyDialog } from "@/composables/useWikidataPropertyDialog"
import {
  parseNoteContentMarkdown,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import { richFrontmatterPropertyRowsFromMarkdown } from "@/utils/richFrontmatterPropertyRowsFromMarkdown"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"

const props = defineProps<{
  contentMarkdown: string
  readOnly?: boolean
  wikiTitles: WikiTitle[]
  noteTitleForWikidataSearch?: string
  noteId?: number
  interactionLocked?: boolean
  isReadmeContext?: boolean
}>()

const emits = defineEmits<{
  "properties-changed": [rows: PropertyRow[]]
  deadWikiLinkClick: [payload: DeadWikiLinkPayload]
  "image-upload-state": [inProgress: boolean]
}>()

const isInteractionLocked = computed(() => props.interactionLocked ?? false)
const headingId = useId()
const insertKeyInputId = `${headingId}-insert-key`
const insertKeyPresetListId = `${headingId}-insert-key-presets`
const isReadOnly = computed(() => props.readOnly ?? false)
provide(
  richFrontmatterIsReadmeContextKey,
  computed(() => props.isReadmeContext ?? false)
)

const parsed = computed(() => parseNoteContentMarkdown(props.contentMarkdown))
const propertyRows = ref<PropertyRow[]>([])
const rowClientIds = usePropertyRowClientIds(propertyRows)
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

const setValidationMessage = (msg: string) => {
  validationMessage.value = msg
}
const clearValidation = () => {
  validationMessage.value = ""
}

const {
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
} = useRichFrontmatterPropertyEditing({
  propertyRows,
  noteId: () => props.noteId,
  isReadmeContext: () => props.isReadmeContext ?? false,
  onPropertiesChanged: (rows) => emits("properties-changed", rows),
  setValidationMessage,
  clearValidation,
  insertKeyInputId,
  insertOpen,
  draftKey,
  draftValue,
  rowSnapshots,
  isReadOnly: () => isReadOnly.value,
  parsedOk: () => parsed.value.ok,
})

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
  onValidationError: setValidationMessage,
  clearValidation,
  onPropertiesChanged: (rows) =>
    emits("properties-changed", filterForEmit(rows)),
  wikidataAssociationDialogRef,
})

const rowKeyInputId = (idx: number) => `${headingId}-row-${idx}-key`
const rowKeyPresetListId = (idx: number) =>
  `${headingId}-row-${idx}-key-presets`

watch(
  () => props.contentMarkdown,
  () => {
    propertyRows.value = parsed.value.ok
      ? richFrontmatterPropertyRowsFromMarkdown(props.contentMarkdown)
      : []
    insertOpen.value = false
    draftKey.value = ""
    draftValue.value = ""
    validationMessage.value = ""
    rowSnapshots.value = {}
    resetDialog()
  },
  { immediate: true }
)

defineExpose({ getPropertyRows, addWikiLinkAsProperty })
</script>

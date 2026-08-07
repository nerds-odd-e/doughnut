<template>
  <Modal v-if="modelValue !== null" @close_request="close">
    <template #body>
      <div v-if="!showCreateForm && !pointingAtExisting" class="flex flex-col gap-3">
        <p class="text-sm opacity-70">
          Dead wiki link: <strong>{{ modelValue.displayText }}</strong>
        </p>
        <div class="flex flex-col gap-2">
          <button
            class="daisy-btn daisy-btn-primary"
            @click="onCreateNewNoteClick"
          >
            Create a new note named "{{ modelValue.displayText }}"
          </button>
          <button
            class="daisy-btn daisy-btn-secondary"
            @click="onPointAtExistingClick"
          >
            {{ pointAtExistingNoteLabel }}
          </button>
        </div>
      </div>
      <NoteNewForm
        v-else-if="showCreateForm && modelValue !== null"
        :notebookId="notebookId"
        :initial-folder="realmLeafFolder(noteRealm)"
        :initial-title="modelValue.displayText"
        :wiki-title-cache-refresh-source-note-id="sourceNoteId"
        :ancestor-folders="noteRealm.ancestorFolders ?? []"
        @close-dialog="close"
      />
      <SearchForm
        v-else-if="pointingAtExisting && modelValue !== null"
        :note="noteRealm.note"
        :dead-link-payload="modelValue"
        :modal-closer="close"
        @close-dialog="close"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, watch } from "vue"
import type { NoteRealm } from "@generated/doughnut-backend-api"
import Modal from "@/components/commons/Modal.vue"
import { realmLeafFolder } from "./useNoteSidebarTree"
import NoteNewForm from "./NoteNewForm.vue"
import SearchForm from "@/components/links/SearchForm.vue"
import type { DeadLinkPayload } from "@/utils/wikiPropertyValueField"
import { primeSoftKeyboard } from "@/utils/focusTarget"

const pointAtExistingNoteLabel = "Point at an existing note"

const props = defineProps<{
  notebookId: number
  noteRealm: NoteRealm
  modelValue: DeadLinkPayload | null
  sourceNoteId: number
}>()

const emit = defineEmits<{
  "update:modelValue": [value: DeadLinkPayload | null]
}>()

const pointingAtExisting = ref(false)
const showCreateForm = ref(false)

watch(
  () => props.modelValue,
  (value) => {
    if (value === null) {
      pointingAtExisting.value = false
      showCreateForm.value = false
    }
  }
)

const onCreateNewNoteClick = () => {
  primeSoftKeyboard()
  showCreateForm.value = true
}

const onPointAtExistingClick = () => {
  primeSoftKeyboard()
  pointingAtExisting.value = true
}

const close = () => {
  emit("update:modelValue", null)
}
</script>

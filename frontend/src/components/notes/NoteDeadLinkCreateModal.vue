<template>
  <Modal v-if="modelValue !== null" @close_request="close">
    <template #body>
      <div v-if="!showCreateForm && !linkingToExisting" class="daisy-flex daisy-flex-col daisy-gap-3">
        <p class="daisy-text-sm daisy-opacity-70">
          Dead link: <strong>{{ modelValue.displayText }}</strong>
        </p>
        <div class="daisy-flex daisy-flex-col daisy-gap-2">
          <button
            class="daisy-btn daisy-btn-primary"
            @click="showCreateForm = true"
          >
            Create a new note named "{{ modelValue.displayText }}"
          </button>
          <button
            class="daisy-btn daisy-btn-secondary"
            @click="linkingToExisting = true"
          >
            Link to an existing note
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
        v-else-if="linkingToExisting && modelValue !== null"
        :note="noteRealm.note"
        :dead-link-payload="modelValue"
        :modal-closer="close"
        @close-dialog="close"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref } from "vue"
import type { NoteRealm } from "@generated/doughnut-backend-api"
import Modal from "@/components/commons/Modal.vue"
import { realmLeafFolder } from "./useNoteSidebarTree"
import NoteNewForm from "./NoteNewForm.vue"
import SearchForm from "@/components/links/SearchForm.vue"
import type { DeadLinkPayload } from "@/utils/wikiPropertyValueField"

defineProps<{
  notebookId: number
  noteRealm: NoteRealm
  modelValue: DeadLinkPayload | null
  sourceNoteId: number
}>()

const emit = defineEmits<{
  "update:modelValue": [value: DeadLinkPayload | null]
}>()

const linkingToExisting = ref(false)
const showCreateForm = ref(false)

const close = () => {
  linkingToExisting.value = false
  showCreateForm.value = false
  emit("update:modelValue", null)
}
</script>

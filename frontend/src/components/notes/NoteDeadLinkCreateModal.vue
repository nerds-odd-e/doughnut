<template>
  <Modal v-if="modelValue !== null" @close_request="close">
    <template #body>
      <div v-if="!showCreateForm && !linkingToExisting" class="daisy-flex daisy-flex-col daisy-gap-3">
        <p class="daisy-text-sm daisy-opacity-70">
          Dead link: <strong>{{ modelValue }}</strong>
        </p>
        <div class="daisy-flex daisy-flex-col daisy-gap-2">
          <button
            class="daisy-btn daisy-btn-primary"
            @click="showCreateForm = true"
          >
            Create a new note named "{{ modelValue }}"
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
        v-else-if="showCreateForm"
        :notebookId="notebookId"
        :initial-folder="realmLeafFolder(noteRealm)"
        :initial-title="modelValue"
        :wiki-title-cache-refresh-source-note-id="sourceNoteId"
        :ancestor-folders="noteRealm.ancestorFolders ?? []"
        @close-dialog="close"
      />
      <SearchForm
        v-else-if="linkingToExisting"
        :note="noteRealm.note"
        :dead-link-payload="deadLinkPayload ?? undefined"
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

const props = defineProps<{
  notebookId: number
  noteRealm: NoteRealm
  modelValue: string | null
  sourceNoteId: number
  deadLinkPayload?: DeadLinkPayload | null
}>()

const emit = defineEmits<{
  "update:modelValue": [value: string | null]
  closed: []
}>()

const linkingToExisting = ref(false)
const showCreateForm = ref(false)

const close = () => {
  linkingToExisting.value = false
  showCreateForm.value = false
  emit("update:modelValue", null)
  emit("closed")
}
</script>

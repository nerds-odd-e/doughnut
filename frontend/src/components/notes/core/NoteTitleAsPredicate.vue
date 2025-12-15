<template>
  <h2 role="title" @click="editingLink = true">
    <NoteTitleComponent v-bind="{ noteTopology: noteTopology, full: true }" />
  </h2>
  <Breadcrumb
    v-if="noteTopology.targetNoteTopology"
    v-bind="{ noteTopology: noteTopology.targetNoteTopology }"
  />
  <Modal v-if="!readonly && editingLink" @close_request="editingLink = false">
    <template #body>
      <LinkNobDialog
        v-bind="{ noteTopology, inverseIcon: false }"
        @close-dialog="editingLink = false"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type { NoteTopology } from "@generated/backend"
import NoteTitleComponent from "./NoteTitleComponent.vue"
import Modal from "../../commons/Modal.vue"
import LinkNobDialog from "../../links/LinkNobDialog.vue"
import Breadcrumb from "../../toolbars/Breadcrumb.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  readonly: { type: Boolean, default: false },
})

const editingLink = ref<boolean>(false)
</script>

<style scoped>
h2 {
  font-size: 1.5rem;
  font-weight: 400;
}
</style>

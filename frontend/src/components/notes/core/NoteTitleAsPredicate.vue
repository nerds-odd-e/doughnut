<template>
  <h2 role="title" @click="editingRelationship = true">
    <NoteTitleComponent v-bind="{ noteTopology: noteTopology, full: true }" />
  </h2>
  <Breadcrumb
    v-if="noteTopology.targetNoteTopology"
    v-bind="{ noteTopology: noteTopology.targetNoteTopology }"
  />
  <Modal v-if="!readonly && editingRelationship" @close_request="editingRelationship = false">
    <template #body>
      <RelationNobDialog
        v-bind="{ noteTopology, inverseIcon: false }"
        @close-dialog="editingRelationship = false"
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
import RelationNobDialog from "../../links/RelationNobDialog.vue"
import Breadcrumb from "../../toolbars/Breadcrumb.vue"

defineProps({
  noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  readonly: { type: Boolean, default: false },
})

const editingRelationship = ref<boolean>(false)
</script>

<style scoped>
h2 {
  font-size: 1.5rem;
  font-weight: 400;
}
</style>

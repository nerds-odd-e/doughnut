<template>
  <h2 role="topic" @click="editingLink = true">
    <NoteTopicComponent v-bind="{ noteTopic: noteTopic }" />
  </h2>
  <Breadcrumb
    v-if="noteTopic.targetNoteTopic"
    v-bind="{ noteTopic: noteTopic.targetNoteTopic }"
  />
  <Modal v-if="!readonly && editingLink" @close_request="editingLink = false">
    <template #body>
      <LinkNobDialog
        v-bind="{ noteTopic, inverseIcon: false, storageAccessor }"
        @close-dialog="editingLink = false"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { PropType, ref } from "vue"
import { NoteTopic } from "@/generated/backend"
import { type StorageAccessor } from "../../../store/createNoteStorage"
import NoteTopicComponent from "./NoteTopicComponent.vue"
import Modal from "../../commons/Modal.vue"
import LinkNobDialog from "../../links/LinkNobDialog.vue"
import Breadcrumb from "../../toolbars/Breadcrumb.vue"

defineProps({
  noteTopic: { type: Object as PropType<NoteTopic>, required: true },
  readonly: { type: Boolean, default: false },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const editingLink = ref<boolean>(false)
</script>

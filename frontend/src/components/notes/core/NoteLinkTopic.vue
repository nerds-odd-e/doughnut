<template>
  <h2 role="topic" @click="editingLink = true">
    <NoteTopicComp v-bind="{ noteTopic: noteTopic }" />
  </h2>
  <Modal v-if="editingLink" @close_request="editingLink = false">
    <template #body>
      <LinkNobDialog
        v-bind="{ noteTopic, inverseIcon: false, storageAccessor }"
        @close-dialog="editingLink = false"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { PropType, ref } from "vue";
import { NoteTopic } from "@/generated/backend";
import { type StorageAccessor } from "../../../store/createNoteStorage";
import NoteTopicComp from "./NoteTopic.vue";
import Modal from "../../commons/Modal.vue";
import LinkNobDialog from "../../links/LinkNobDialog.vue";

defineProps({
  noteTopic: { type: Object as PropType<NoteTopic>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const editingLink = ref<boolean>(false);
</script>

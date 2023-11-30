<template>
  <table class="table">
    <thead>
      <tr>
        <th scope="col">Stem</th>
        <th scope="col">Feedback</th>
        <th scope="col">Comment</th>
        <th scope="col">Operation</th>
      </tr>
    </thead>
    <tbody>
      <SuggestedQuestionRow
        v-for="(suggestedQuestion, index) in suggestedQuestions"
        v-bind="{ suggestedQuestion }"
        :key="index"
        @dblclick="editingIndex = index"
        @duplicated="$emit('duplicated', $event)"
      />
    </tbody>
  </table>
  <Modal
    v-if="editingIndex !== undefined"
    @close_request="editingIndex = undefined"
  >
    <template #body>
      <SuggestedQuestionEdit
        v-model="localSuggestedQuestions[editingIndex]"
        @update:model-value="editingIndex = undefined"
      />
    </template>
  </Modal>
</template>

<script lang="ts">
import { PropType } from "vue";
import SuggestedQuestionRow from "./SuggestedQuestionRow.vue";
import Modal from "../commons/Modal.vue";

export default {
  props: {
    suggestedQuestions: {
      type: Array as PropType<Generated.SuggestedQuestionForFineTuning[]>,
      required: true,
    },
  },
  emits: ["duplicated"],
  watch: {
    suggestedQuestions: {
      handler() {
        this.localSuggestedQuestions = this.suggestedQuestions;
      },
      deep: true,
    },
  },
  data() {
    return {
      localSuggestedQuestions: this.suggestedQuestions,
      editingIndex: undefined as undefined | number,
    };
  },

  components: { Modal, SuggestedQuestionRow },
};
</script>

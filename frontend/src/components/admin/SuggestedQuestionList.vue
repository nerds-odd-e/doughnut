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
  <Popup1 v-if="editingIndex" @popup-done="editingIndex = undefined">
    <SuggestedQuestionEdit v-model="localSuggestedQuestions[editingIndex]" />
  </Popup1>
</template>

<script lang="ts">
import { PropType } from "vue";
import SuggestedQuestionRow from "./SuggestedQuestionRow.vue";
import Popup1 from "../commons/Popups/Popup1.vue";

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

  components: { Popup1, SuggestedQuestionRow },
};
</script>

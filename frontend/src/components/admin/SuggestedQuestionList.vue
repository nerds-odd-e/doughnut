<template>
  <table class="table">
    <thead>
      <tr>
        <th scope="col">Stem</th>
        <th scope="col">Feedback</th>
        <th scope="col">Comment</th>
        <th scope="col">Operation</th>
        <th scope="col">Is Duplicated</th>
      </tr>
    </thead>
    <tbody>
      <SuggestedQuestionRow
        v-for="(suggestedQuestion, index) in suggestedQuestions"
        v-bind="{ suggestedQuestion, index }"
        :key="index"
        @dblclick="editingIndex = index"
        @duplicated="$emit('duplicated', $event)"
      />
    </tbody>
  </table>
  <Popup
    :show="editingIndex != undefined"
    @popup-done="editingIndex = undefined"
  >
    <SuggestedQuestionEdit
      v-if="editingIndex !== undefined"
      v-model="localSuggestedQuestions[editingIndex]"
    />
  </Popup>
</template>

<script lang="ts">
import { PropType } from "vue";
import SuggestedQuestionRow from "./SuggestedQuestionRow.vue";
import Popup from "../commons/Popups/Popup.vue";

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

  components: { Popup, SuggestedQuestionRow },
};
</script>

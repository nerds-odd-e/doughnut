<template>
  <tr>
    <td>{{ suggestedQuestion.preservedQuestion.stem }}</td>
    <td>{{ suggestedQuestion.positiveFeedback ? "Positive" : "Negative" }}</td>
    <td>{{ suggestedQuestion.comment }}</td>
    <td>
      <button
        v-if="!suggestedQuestion.positiveFeedback"
        class="btn btn-primary"
        @click="duplicateQuestion(suggestedQuestion)"
      >
        Duplicate
      </button>
    </td>
  </tr>
</template>

<script lang="ts">
import { PropType } from "vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default {
  setup() {
    return useLoadingApi();
  },
  props: {
    suggestedQuestion: {
      type: Object as PropType<Generated.SuggestedQuestionForFineTuning>,
      required: true,
    },
  },
  emits: ["duplicated"],
  methods: {
    async duplicateQuestion(
      suggested: Generated.SuggestedQuestionForFineTuning,
    ) {
      const duplicated =
        await this.api.fineTuning.duplicateSuggestedQuestionForFineTuning(
          suggested.id,
        );
      this.$emit("duplicated", duplicated);
    },
  },
};
</script>

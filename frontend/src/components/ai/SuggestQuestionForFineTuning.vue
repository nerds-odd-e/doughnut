<template>
  <h2>Suggest This Question For AI Fine Tuning</h2>
  <p>
    <i
      >Sending this question for fine tuning the question generation model will
      make this note and question visible to admin. Are you sure?</i
    >
  </p>
  <div>
    <TextInput
      field="comment"
      v-model="comment"
      placeholder="Add a comment about the question"
    />
  </div>
  <button class="btn btn-success" @click="suggestQuestionForFineTuning">
    OK
  </button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import asPopup from "../commons/Popups/asPopup";
import TextInput from "../form/TextInput.vue";

export default defineComponent({
  inheritAttrs: false,
  setup() {
    return { ...useLoadingApi(), ...asPopup() };
  },
  props: {
    quizQuestion: {
      type: Object as PropType<Generated.QuizQuestion>,
      required: true,
    },
  },
  data() {
    return {
      comment: "" as string,
    };
  },
  methods: {
    async suggestQuestionForFineTuning() {
      await this.api.reviewMethods.suggestQuestionForFineTuning(
        this.quizQuestion.quizQuestionId,
        {
          isPositiveFeedback: false,
          comment: this.comment,
        },
      );
      this.popup.done(null);
    },
  },
  components: { TextInput },
});
</script>

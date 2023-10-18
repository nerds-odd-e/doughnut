<template>
  <h2>Suggest This Question For AI Fine Tuning</h2>
  <p>
    <i
      >Sending this question for fine tuning the question generation model will
      make this note and question visible to admin. Are you sure?</i
    >
  </p>
  <div>
    <label> This question is: </label>
    <button
      class="positive-feedback-btn feedback-btn"
      :class="{ selected: isPositiveFeedback }"
      @click="markQuestionAsPositive"
    >
      👍 Good
    </button>
    <button
      class="negative-feedback-btn feedback-btn"
      :class="{ selected: isPositiveFeedback === false }"
      @click="markQuestionAsNegative"
    >
      👎 Bad
    </button>
  </div>
  <TextInput
    id="feedback-comment"
    field="comment"
    v-model="comment"
    placeholder="Add a comment about the question"
  />
  <div class="feedback-actions-container">
    <button
      class="suggest-fine-tuning-ok-btn btn btn-success"
      @click="suggestQuestionForFineTuning"
    >
      OK
    </button>
  </div>
</template>
<script setup lang="ts">
import { ref } from "vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import asPopup from "../commons/Popups/asPopup";

const isPositiveFeedback = ref<boolean | null>(null);
const comment = ref<string>("");
const { api } = useLoadingApi();
const { popup } = asPopup();

const props = defineProps<{
  quizQuestion: Generated.QuizQuestion | undefined;
}>();

const { quizQuestion } = props;

async function suggestQuestionForFineTuning() {
  await api.reviewMethods.suggestQuestionForFineTuning(
    quizQuestion!.quizQuestionId,
    {
      isPositiveFeedback: isPositiveFeedback.value ?? false,
      comment: comment.value,
      isDuplicated: false,
    },
  );
  popup.done(null);
}

function markQuestionAsPositive() {
  isPositiveFeedback.value = true;
}

function markQuestionAsNegative() {
  isPositiveFeedback.value = false;
}
</script>
<script lang="ts">
export default {
  name: "SuggestQuestionForFineTuningIntegrated",
  inheritAttrs: false,
  customOptions: {},
};
</script>
<style scoped>
.container {
  padding: 20px;
}
.feedback-btn {
  background-color: #007bff;
  color: white;
  padding: 5px;
  margin: 5px;
  border-radius: 5px;
}
.positive-feedback-btn.feedback-btn.selected {
  background-color: green;
}
.negative-feedback-btn.feedback-btn.selected {
  background-color: red;
}
.feedback-actions-container {
  display: flex;
  align-items: center;
}
</style>

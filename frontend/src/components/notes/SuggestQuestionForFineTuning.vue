<template>
  <div>
    <h2>Suggest This Question For AI Fine Tuning</h2>
    <p>
      <i
        >Sending this question for fine tuning the question generation model
        will make this note and question visible to admin. Are you sure?</i
      >
    </p>
    <div>
      <button
        class="positive-feedback-btn feedback-btn"
        :class="{ selected: isPositive }"
        @click="markQuestionAsPositive"
      >
        Positive
      </button>
      <button
        class="negative-feedback-btn feedback-btn"
        :class="{ selected: isPositive === false }"
        @click="markQuestionAsNegative"
      >
        Negative
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
      <div
        class="suggestion-sent-successfully-message"
        v-if="suggestionSubmittedSuccessfully"
      >
        Feedback sent successfully!
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, defineProps } from "vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

const isPositive = ref<boolean | null>(null);
const comment = ref<string>("");
const suggestionSubmittedSuccessfully = ref<boolean>(true);

const { api } = useLoadingApi();

const props = defineProps<{
  quizQuestion: Generated.QuizQuestion | undefined;
}>();

const { quizQuestion } = props;

async function suggestQuestionForFineTuning() {
  try {
    await api.reviewMethods.suggestQuestionForFineTuning(
      quizQuestion!.quizQuestionId,
      {
        isPositive: isPositive.value ?? false,
        comment: comment.value,
      },
    );
    suggestionSubmittedSuccessfully.value = true;
  } catch (err) {
    suggestionSubmittedSuccessfully.value = false;
  }
}

function markQuestionAsPositive() {
  isPositive.value = true;
}

function markQuestionAsNegative() {
  isPositive.value = false;
}
</script>
<script lang="ts">
export default {
  name: "SuggestQuestionForFineTuningIntegrated",
  inheritAttrs: false,
  customOptions: {},
};
</script>
<style>
.feedback-btn.selected {
  background-color: green;
  color: white;
}

.feedback-actions-container {
  display: flex;
}
</style>

<template>
  <div>
    <TextArea
      :rows="2"
      field="stem"
      v-model="formData.stem"
    /><br />
  <div v-for="(_, index) in formData.choices" :key="index">
    <TextArea
      :field="'choice ' + index"
      :rows="1"
      v-model="formData.choices[index]"
    />
    <br />
  </div>

  <TextInput
    rows="2"
    field="correctChoiceIndex"
    v-model="formData.correctAnswerIndex"
  /><br />

  <button
    @click="addChoice"
    :disabled="
        formData.choices.length >= maximumNumberOfChoices
      "
  >
    +
  </button>
  <button
    @click="removeChoice"
    :disabled="
        formData.choices.length <= minimumNumberOfChoices
      "
  >
    -
  </button>
  <button @click="refineQuestion" :disabled="!dirty">Refine</button>
  <button @click="generateQuestionByAI" :disabled="dirty">
    Generate by AI
  </button>
  <button @click="submitQuestion" :disabled="!isValidQuestion">Submit</button>
  </div>
</template>

<script>
import useLoadingApi from "@/managedApi/useLoadingApi.ts";
import CheckInput from "@/components/form/CheckInput.vue";
import TextInput from "@/components/form/TextInput.vue";
import TextArea from "@/components/form/TextArea.vue";

export default {
  setup() {
    return useLoadingApi()
  },
  props: { question: Object },
  components: {TextArea, CheckInput, TextInput },
  data() {
    console.log(this.question);
    console.log(this.question.stem);
    const {
      correctAnswerIndex,
    } = this.question;
    const {
      stem,
      choices,
    } = this.question.quizQuestion.multipleChoicesQuestion;
    return {
      formData: {
        correctAnswerIndex,
        stem,
        choices,
      },
      errors: {},
    }
  },
}
</script>

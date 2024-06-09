<template>
  <h3>Assessment For {{ topicConstructor }}</h3>
  <div>
    <div v-if="errors != ''">
      {{ errors }}
    </div>
    <div v-else-if="currentQuestion < quizQuestions.length">
      <p role="question">
        {{ quizQuestions[currentQuestion]?.multipleChoicesQuestion.stem }}
      </p>
      <button
        v-for="(choice, index) in quizQuestions[currentQuestion]
          ?.multipleChoicesQuestion.choices"
        :key="index"
        @click="selectAnswer()"
      >
        {{ choice }}
      </button>
    </div>
    <div v-else>
      <p>End of assessment</p>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { QuizQuestion } from "@/generated/backend";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  props: {
    notebookId: { type: Number, required: true },
  },
  computed: {
    topicConstructor() {
      return this.$route.query.topic;
    },
  },
  data() {
    return {
      quizQuestions: [] as QuizQuestion[],
      answered: false,
      currentQuestion: 0,
      errors: "",
    };
  },
  created() {
    this.generateAssessmentQuestions();
  },
  methods: {
    selectAnswer() {
      this.nextQuestion();
    },
    nextQuestion() {
      this.currentQuestion += 1;
    },
    generateAssessmentQuestions() {
      this.managedApi.restAssessmentController
        .generateAssessmentQuestions(this.notebookId)
        .then((response) => {
          this.quizQuestions = response;
        })
        .catch((res) => {
          this.errors = res.body.message;
        });
    },
  },
});
</script>

<template>
  <h3>Assessment For {{ topicConstructor }}</h3>
  <p v-if="fetchingAssessment">Generating Questions.. Please Wait A Moment.</p>
  <p v-if="noAssessmentQuestions">Insufficient notes to create assessment!</p>

  <div v-else>
    <div v-for="(question, index) in result" :key="index">
      <p>
        <strong>Question {{ index + 1 }}:</strong>
        {{ question.multipleChoicesQuestion.stem }}
      </p>
      <ol>
        <li
          v-for="(choice, choiceIndex) in question.multipleChoicesQuestion
            .choices"
          :key="choiceIndex"
        >
          {{ choice }}
        </li>
      </ol>
      <hr />
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
      fetchingAssessment: false,
      noAssessmentQuestions: false,
      result: [] as QuizQuestion[],
      errors: {},
    };
  },
  mounted() {
    this.fetchingAssessment = true;
    this.generateAssessmentQuestions();
  },
  methods: {
    generateAssessmentQuestions() {
      this.managedApi.restAssessmentController
        .generateAiQuestions(this.notebookId)
        .then((response) => {
          if (!response || response.length === 0) {
            this.noAssessmentQuestions = true;
          } else {
            this.result = response;
            this.fetchingAssessment = false;
          }
        })
        .catch((res) => {
          this.errors = res;
          this.fetchingAssessment = false;
        });
    },
  },
  watch: {
    fetchingAssessment(newValue, oldValue) {
      if (newValue !== oldValue) {
        this.fetchingAssessment = newValue;
      }
    },
  },
});
</script>

<template>
  <h3>Assessment For {{ topicConstructor }}</h3>
  <div>
    <div v-if="errors != ''">
      {{ errors }}
    </div>
    <QuizQuestionComp
      v-else-if="currentQuestion < quizQuestions.length"
      :quiz-question="quizQuestions[currentQuestion]!"
      @answered="questionAnswered"
    />
    <div v-else>
      <p>End of assessment</p>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { QuizQuestion } from "@/generated/backend";
import QuizQuestionComp from "../components/review/QuizQuestion.vue";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  props: {
    notebookId: { type: Number, required: true },
  },
  computed: {
    topicConstructor() {
      return this.$route.query?.topic;
    },
  },
  components: {
    QuizQuestionComp,
  },
  data() {
    return {
      quizQuestions: [] as QuizQuestion[],
      currentQuestion: 0,
      errors: "",
    };
  },
  created() {
    this.generateAssessmentQuestions();
  },
  methods: {
    questionAnswered() {
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

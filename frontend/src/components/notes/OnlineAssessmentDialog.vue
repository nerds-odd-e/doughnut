<template>
  <div>
    <div v-if="currentQuestion < quizQuestions.length">
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
      <p>End of questions</p>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { QuizQuestion } from "@/generated/backend";

export default defineComponent({
  data() {
    return {
      quizQuestions: [] as QuizQuestion[],
      answered: false,
      currentQuestion: 0,
    };
  },
  created() {
    this.quizQuestions = this.getQuizQuestions();
  },
  methods: {
    selectAnswer() {
      this.nextQuestion();
    },
    nextQuestion() {
      this.currentQuestion += 1;
    },
    getQuizQuestions() {
      return [
        {
          id: 1,
          multipleChoicesQuestion: {
            stem: "Where in the world is Singapore?",
            choices: ["Asia", "euro"],
          },
          headNote: {
            noteTopic: {
              id: 1,
              topicConstructor: "Singapore",
            },
            updatedAt: "2022-06-01T12:00:00Z",
            id: 1,
            createdAt: "2022-06-01T12:00:00Z",
          },
          approved: true,
          reviewed: true,
        },
        {
          id: 2,
          multipleChoicesQuestion: {
            stem: "Most famous food of Vietnam?",
            choices: ["Pho", "bread"],
          },
          headNote: {
            noteTopic: {
              id: 2,
              topicConstructor: "Vietnam",
            },
            updatedAt: "2022-06-01T12:00:00Z",
            id: 1,
            createdAt: "2022-06-01T12:00:00Z",
          },
          approved: true,
          reviewed: true,
        },
      ];
    },
  },
});
</script>

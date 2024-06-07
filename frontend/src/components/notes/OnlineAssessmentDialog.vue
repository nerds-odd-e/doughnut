<template>
  <div>
    <div v-if="currentQuestion === 1">
      <p role="question">Where in the world is Singapore?</p>
      <button @click="nextQuestion">Asia</button>
    </div>
    <div v-else-if="currentQuestion === 2">
      <p role="question">Most famous food of Vietnam?</p>
      <button @click="nextQuestion">Pho</button>
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
      quizQuestions: undefined as QuizQuestion[] | undefined,
      answered: false,
      currentQuestion: 1,
    };
  },
  created() {
    this.quizQuestions = this.getQuizQuestions();
  },
  methods: {
    nextQuestion() {
      this.currentQuestion += 1;
    },
    getQuizQuestions() {
      return [
        {
          id: 1,
          multipleChoicesQuestion: {
            stem: "What is the capital of France?",
            choices: ["Paris", "London", "Berlin", "Rome"],
          },
          headNote: {
            noteTopic: {
              id: 1,
              topicConstructor: "France",
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
            stem: "Who painted the Mona Lisa?",
            choices: [
              "Leonardo da Vinci",
              "Pablo Picasso",
              "Vincent van Gogh",
              "Michelangelo",
            ],
          },
          headNote: {
            noteTopic: {
              id: 1,
              topicConstructor: "France",
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

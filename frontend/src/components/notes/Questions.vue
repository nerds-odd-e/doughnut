<template>
  <div>
    <table class="question-table">
      <thead>
        <tr>
          <th>Question ID</th>
          <th>Question Text</th>
          <th>A</th>
          <th>B</th>
          <th>C</th>
          <th>D</th>
          <th>Approved?</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="question in questions" :key="question.id">
          <td>{{ question.id }}</td>
          <td>{{ question.multipleChoicesQuestion.stem }}</td>
          <template v-if="question.multipleChoicesQuestion.choices">
            <td
              v-for="(choice, index) in question.multipleChoicesQuestion
                .choices"
              :key="index"
            >
              {{ choice }}
            </td>
          </template>
          <td><input type="checkbox" v-model="question.approved" /></td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import { defineComponent } from "vue";
import useLoadingApi from "@/managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
  },
  data() {
    return {
      currentQuestion: 1,
      questions: [],
    };
  },
  methods: {
    nextQuestion() {
      if (this.currentQuestion < 2) {
        this.currentQuestion += 1;
      }
    },
    fetchQuestions() {
      this.managedApi.restQuizQuestionController
        .getAllQuizQuestionByNote(this.noteId)
        .then((questions) => {
          this.questions = questions;
        });
    },
  },
  mounted() {
    this.fetchQuestions();
  },
});
</script>

<style scoped>
.question-table {
  border-collapse: collapse;
  width: 100%;
}

.question-table th,
.question-table td {
  border: 1px solid #dddddd;
  text-align: left;
  padding: 8px;
}

.question-table th {
  background-color: #f2f2f2;
}
</style>

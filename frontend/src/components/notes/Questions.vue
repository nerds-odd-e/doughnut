<template>
  <div>
    <PopButton btn-class="btn btn-primary" title="Add Question">
      <template #default="{ closer }">
        <NoteAddQuestion
          v-bind="{ note }"
          @close-dialog="
            closer($event);
            questionAdded($event);
          "
        />
      </template>
    </PopButton>
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
              :class="{ 'correct-choice': index === 0 }"
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

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note, QuizQuestion } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    note: {
      type: Object as PropType<Note>,
      required: true,
    },
  },
  data() {
    return {
      questions: [] as QuizQuestion[],
    };
  },
  methods: {
    fetchQuestions() {
      this.managedApi.restQuizQuestionController
        .getAllQuizQuestionByNote(this.note.id)
        .then((questions) => {
          this.questions = questions;
        });
    },
    questionAdded(newQuestion: QuizQuestion) {
      if (newQuestion == null) {
        return;
      }
      this.questions.push(newQuestion);
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

.correct-choice {
  background-color: #4caf50;
}
</style>

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
    <table class="question-table mt-2">
      <thead>
        <tr>
          <th>Approved</th>
          <th>Question Text</th>
          <th>A</th>
          <th>B</th>
          <th>C</th>
          <th>D</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="question in questions"
          :key="question.multipleChoicesQuestion.stem"
        >
          <td><input type="checkbox" /></td>
          <td>{{ question.multipleChoicesQuestion.stem }}</td>
          <template v-if="question.multipleChoicesQuestion.choices">
            <td
              v-for="(choice, index) in question.multipleChoicesQuestion
                .choices"
              :class="{
                'correct-choice': index === question.correctChoiceIndex,
              }"
              :key="index"
            >
              {{ choice }}
            </td>
          </template>
        </tr>
      </tbody>
    </table>

    <input type="submit" value="Approve" class="btn btn-primary" />
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { MCQWithAnswer, Note } from "@/generated/backend";
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
      questions: [] as MCQWithAnswer[],
    };
  },
  methods: {
    fetchQuestions() {
      this.managedApi.restQuizQuestionController
        .getAllQuestionByNote(this.note.id)
        .then((questions) => {
          this.questions = questions;
        });
    },
    questionAdded(newQuestion: MCQWithAnswer) {
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

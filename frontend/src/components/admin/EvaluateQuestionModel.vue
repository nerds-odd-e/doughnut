<template>
  <div>
    <table>
      <thead>
        <tr>
          <th>Question Model</th>
          <th>Score</th>
          <th>Evaluate</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="questionModel in questionModels" :key="questionModel.id">
          <td>{{ questionModel.name }}</td>
          <td>{{ questionModel.score ? questionModel.score : "-" }}%</td>
          <td>
            <button @click="triggerEvaluation(questionModel)">Trigger</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
<script lang="ts">
import useLoadingApi from "../../managedApi/useLoadingApi";

export default {
  setup() {
    return useLoadingApi();
  },
  data() {
    return {
      questionModels: [
        {
          id: "question_model_1",
          name: "QM_1.8",
          score: null,
        },
      ],
    };
  },
  methods: {
    async triggerEvaluation(model) {
      model.score = await this.api.ai.evaluateQuestionModel();
    },
  },
};
</script>

<style scoped></style>

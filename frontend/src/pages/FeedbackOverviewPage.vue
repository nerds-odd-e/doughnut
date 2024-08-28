<template>
  <ContainerPage
    v-bind="{
      contentExists: true,
      title: 'Feedback',
    }"
  >
    <h2>There is no feedback currently.</h2>
    <table class="feedback-table mt-2">
      <thead>
      <tr>
        <th>Question</th>
        <th>Name</th>
      </tr>
      </thead>
      <tbody>
      <tr>
        <td>Question 5</td>
        <td>Frank Dorssers</td>
      </tr>
      </tbody>
    </table>
  </ContainerPage>
</template>

<script setup lang="ts">
import ContainerPage from "@/pages/commons/ContainerPage.vue"
</script>

<style scoped>
.feedback-table {
  border-collapse: collapse;
  width: 100%;
}

.feedback-table th,
.feedback-table td {
  border: 1px solid #dddddd;
  text-align: left;
  padding: 8px;
}

.feedback-table th {
  background-color: #f2f2f2;
}
</style>


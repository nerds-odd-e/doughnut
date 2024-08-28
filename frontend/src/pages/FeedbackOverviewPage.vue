<template>
  <ContainerPage
    v-bind="{
      contentExists: true,
      title: 'Feedback',
    }"
  >
    <h2>There is no feedback currently.</h2>
    <table class="assessment-table mt-2">
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



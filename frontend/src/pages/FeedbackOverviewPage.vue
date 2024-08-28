<template>
  <ContainerPage
    v-bind="{
      contentExists: true,
      title: 'Feedback',
    }"
  >
    <h2>There is no feedback currently.</h2>
  </ContainerPage>
</template>

<script setup lang="ts">
import ContainerPage from "@/pages/commons/ContainerPage.vue"
</script>



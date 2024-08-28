<template>
  <ContainerPage
    v-bind="{
      contentExists: true,
      title: 'Feedback conversation',
    }"
  >
    <h2>Empty conversation</h2>
  </ContainerPage>
</template>

<script setup lang="ts">
import ContainerPage from "@/pages/commons/ContainerPage.vue"
</script>

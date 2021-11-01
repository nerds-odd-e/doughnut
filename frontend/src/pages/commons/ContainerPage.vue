<template>
  <LoadingPage v-bind="{ loading, contentExists, inContainer: true }">
    <h2 v-if="!!title">{{title}}</h2>
    <slot/>
  </LoadingPage>
</template>

<script setup>
import LoadingPage from "./LoadingPage.vue";

const props = defineProps({title: String, loading: Boolean, contentExists: Boolean})
</script>

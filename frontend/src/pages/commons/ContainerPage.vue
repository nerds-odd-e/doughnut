<template>
  <div class="container">
    <LoadingPage v-bind="{ contentExists }">
      <h2 v-if="!!title">{{ title }}</h2>
      <slot />
    </LoadingPage>
  </div>
</template>

<script setup>
import LoadingPage from "./LoadingPage.vue";

const props = defineProps({
  title: String,
  contentExists: Boolean,
});
</script>

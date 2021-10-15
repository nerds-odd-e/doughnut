<template>
  <div class="container">
    <h2 v-if="!!title">{{title}}</h2>
    <LoadingPage v-bind="{ loading, contentExists }">
      <slot/>
    </LoadingPage>
  </div>
</template>

<script setup>
import LoadingPage from "./LoadingPage.vue";

const props = defineProps({title: String, loading: Boolean, contentExists: Boolean})
</script>

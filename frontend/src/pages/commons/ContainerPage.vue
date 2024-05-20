<template>
  <div class="container">
    <ContentLoader v-if="!contentExists" />
    <template v-else>
      <h2 v-if="!!title">{{ title }}</h2>
      <slot />
    </template>
  </div>
</template>

<script setup>
import ContentLoader from "@/components/commons/ContentLoader.vue";

const props = defineProps({
  title: String,
  contentExists: Boolean,
});
</script>

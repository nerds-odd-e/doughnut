<template>
  <teleport v-if="title" to="#head-status">
    <h2>{{ title }}</h2>
  </teleport>

  <div class="container mt-3">
    <ContentLoader v-if="!contentExists" />
    <template v-else>
      <slot />
    </template>
  </div>
</template>

<script setup>
import ContentLoader from "@/components/commons/ContentLoader.vue"

defineProps({
  title: String,
  contentExists: Boolean,
})
</script>

<template>
  <teleport v-if="teleportTargetExists && title" :to="teleportTargetName">
    <h2>{{ title }}</h2>
  </teleport>

  <div class="container mt-3">
    <ContentLoader v-if="!contentExists" />
    <template v-else>
      <slot />
    </template>
  </div>
</template>

<script setup lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"

const teleportTargetName = "#head-status"
const teleportTargetExists = !!document.querySelector(teleportTargetName)

defineProps({
  title: String,
  contentExists: Boolean,
})
</script>

<template>
  <TeleportToHeadStatus v-if="title">
    <h2>{{ title }}</h2>
  </TeleportToHeadStatus>

  <div class="container-fluid px-0 vh-100">
    <ContentLoader v-if="!contentExists" />
    <template v-else>
      <slot />
    </template>
  </div>
</template>

<script setup lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"
import TeleportToHeadStatus from "@/pages/commons/TeleportToHeadStatus.vue"

defineProps({
  title: String,
  contentExists: Boolean,
})
</script>

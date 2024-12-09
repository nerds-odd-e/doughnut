<template>
  <TeleportToHeadStatus v-if="title">
    <h2 class="fs-4 daisy-text-2xl">{{ title }}</h2>
  </TeleportToHeadStatus>

  <div :class="[
    containerClass,
    'daisy-container daisy-mx-auto',
    { 'daisy-h-full daisy-min-h-full': props.fullHeight }
  ]">
    <ContentLoader v-if="!contentLoaded" />
    <template v-else>
      <slot />
    </template>
  </div>
</template>

<script setup lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"
import TeleportToHeadStatus from "@/pages/commons/TeleportToHeadStatus.vue"
import { computed } from "vue"

interface Props {
  title?: string
  contentLoaded?: boolean
  fullHeight?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  fullHeight: false,
  contentLoaded: true,
})

const containerClass = computed(() => ({
  "container-full-height": props.fullHeight,
  "container mt-3": !props.fullHeight,
}))
</script>
<style scoped>
.container-full-height {
  height: 100%;
  min-height: 100%;
}
</style>


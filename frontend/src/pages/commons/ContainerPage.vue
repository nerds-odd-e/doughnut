<template>
  <TeleportToHeadStatus v-if="title">
    <h2 class="fs-4 daisy-text-2xl">{{ title }}</h2>
  </TeleportToHeadStatus>

  <div :class="[
    'daisy-mx-auto daisy-min-w-0',
    { 'daisy-h-full daisy-min-h-full': props.fullHeight },
    { 'daisy-container daisy-mt-3': !props.fullHeight }
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

interface Props {
  title?: string
  contentLoaded?: boolean
  fullHeight?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  fullHeight: false,
  contentLoaded: true,
})
</script>


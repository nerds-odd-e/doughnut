<template>
  <TeleportToHeadStatus v-if="title">
    <h2>{{ title }}</h2>
  </TeleportToHeadStatus>

  <div :class="containerClass">
    <ContentLoader v-if="!contentExists" />
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
  contentExists?: boolean
  fluid?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  fluid: false,
})

const containerClass = computed(() => ({
  "container-fluid px-0": props.fluid,
  "container mt-3": !props.fluid,
}))
</script>

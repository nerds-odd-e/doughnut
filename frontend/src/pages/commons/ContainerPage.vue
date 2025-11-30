<template>
  <GlobalBar
    v-if="title && apiStatus && currentUser"
    v-bind="{ apiStatus, user: currentUser }"
  >
    <template #status>
      <h2 class="fs-4 daisy-text-2xl">{{ title }}</h2>
    </template>
  </GlobalBar>

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
import { inject, type Ref } from "vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import type { User } from "@generated/backend"
import type { ApiStatus } from "@/managedApi/ApiStatusHandler"

interface Props {
  title?: string
  contentLoaded?: boolean
  fullHeight?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  fullHeight: false,
  contentLoaded: true,
})

const currentUser = inject<Ref<User | undefined>>("currentUser")
const apiStatus = inject<Ref<ApiStatus>>("apiStatus")
</script>


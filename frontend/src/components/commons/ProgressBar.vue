<template>
  <GlobalBar
    v-if="title && apiStatus"
    v-bind="{ apiStatus, user: currentUser }"
  >
    <template #status>
      <div class="daisy-flex-shrink-0">
        <slot name="buttons" />
      </div>
      <div class="daisy-flex-grow" @click.prevent="$emit('showMore')">
        <div
          :class="['daisy-progress-bar', { thin : $slots.default !== undefined }]"
          v-if="toRepeatCount !== null"
        >
          <span
            class="progress"
            :style="`width: ${(finished * 100) / (finished + toRepeatCount)}%`"
          >
          </span>
          <span class="progress-text">
            {{ title }}{{ finished }}/{{ finished + toRepeatCount }}
          </span>
        </div>
      </div>
    </template>
  </GlobalBar>
</template>

<script setup lang="ts">
import { inject, type Ref } from "vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import type { User } from "@generated/backend"
import type { ApiStatus } from "@/managedApi/ApiStatusHandler"

defineProps({
  finished: { type: Number, required: true },
  toRepeatCount: { type: Number, required: true },
  title: String,
})

defineEmits(["resume", "showMore"])

defineSlots<{
  default?: () => Element | Element[]
  buttons?: () => Element | Element[]
}>()

const currentUser = inject<Ref<User | undefined>>("currentUser")
const apiStatus = inject<Ref<ApiStatus>>("apiStatus")
</script>

<style lang="scss" scoped>
.daisy-progress-bar {
  width: 100%;
  background-color: gray;
  height: 25px;
  border-radius: 10px;
  position: relative;

  &.thin {
    height: 5px;

    .progress-text {
      display: none;
    }
  }
}

.progress {
  background-color: blue;
  height: 100%;

  &.secondary {
    background-color: #4CAF50;
  }
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
}
</style>

<template>
  <div class="daisy-flex-shrink-0">
    <slot name="buttons" />
  </div>
  <div class="daisy-flex-grow" @click.prevent="$emit('showSettings')">
    <div
      :class="['daisy-progress-bar', { thin : $slots.default !== undefined, 'diligent-mode': diligentMode }]"
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
      <span v-if="$slots.cogIcon" class="cog-icon">
        <slot name="cogIcon" />
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps({
  finished: { type: Number, required: true },
  toRepeatCount: { type: Number, required: true },
  title: String,
  diligentMode: { type: Boolean, default: false },
})

defineEmits(["resume", "showSettings"])

defineSlots<{
  default?: () => Element | Element[]
  buttons?: () => Element | Element[]
  cogIcon?: () => Element | Element[]
}>()
</script>

<style lang="scss" scoped>
.daisy-progress-bar {
  width: 100%;
  background-color: gray;
  height: 2.5rem;
  border-radius: 10px;
  position: relative;

  &.thin {
    height: 5px;

    .progress-text {
      display: none;
    }
  }

  &.diligent-mode {
    background-color: #dc2626;
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

.cog-icon {
  position: absolute;
  top: 50%;
  right: 0.5rem;
  transform: translateY(-50%);
  color: white;
  display: flex;
  align-items: center;
  cursor: pointer;
}
</style>

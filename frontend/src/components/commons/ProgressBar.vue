<template>
  <div class="paused" v-if="paused" @click="$emit('resume')">
    <a title="Go back to review">
      <SvgResume width="50" height="50" />
    </a>
  </div>
  <teleport v-if="title" to="#head-status">
    <div class="flex-shrink-0">
      <slot name="buttons" />
    </div>
    <div class="flex-grow-1 review-info-bar-right" @click.prevent="goHome()">
      <span
        :class="`progress-bar ${!!$slots.default ? 'thin' : ''}`"
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
      </span>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import usePopups from "@/components/commons/Popups/usePopups";
import SvgResume from "@/components/svgs/SvgResume.vue";

defineProps({
  paused: { type: Boolean, required: true },
  finished: { type: Number, required: true },
  toRepeatCount: { type: Number, required: true },
  title: String,
});

defineEmits(["resume"]);

const goHome = async () => {
  if (await usePopups().popups.confirm("Confirm to leave the reviewing?")) {
    window.location.href = "/";
  }
};
</script>

<style lang="scss" scoped>
.review-info-bar-right {
  flex-grow: 1;
}

.progress-bar {
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
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
}
</style>

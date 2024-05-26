<template>
  <div class="paused" @click="$emit('resume')">
    <a title="Go back to review">
      <SvgResume width="50" height="50" />
    </a>
  </div>
</template>

<script setup lang="ts">
import SvgResume from "@/components/svgs/SvgResume.vue";

defineProps({
  finished: { type: Number, required: true },
  toRepeatCount: { type: Number, required: true },
});

defineEmits(["resume"]);
</script>

<style lang="scss" scoped>
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

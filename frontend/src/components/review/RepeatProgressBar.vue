<template>
  <ProgressBar v-bind="{ title: `Repetation: `, finished, toRepeatCount }">
    <template #buttons>
      <ViewLastResultButton
        v-bind="{ hasLastResult }"
        @viewLastResult="$emit('viewLastResult')"
      />
    </template>
    <template #default v-if="$slots.default">
      <slot />
    </template>
  </ProgressBar>
</template>

<script>
import ViewLastResultButton from "./ViewLastResultButton.vue";
import ProgressBar from "../commons/ProgressBar.vue";


export default {
  components: { ViewLastResultButton, ProgressBar },
  props: {
    finished: Number,
    toRepeatCount: Number,
    hasLastResult: Boolean,
  },
  emits: ["viewLastResult"],
  methods: {},
};
</script>

<style lang="scss" scoped>
.review-info-bar {
  display: flex;
}

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

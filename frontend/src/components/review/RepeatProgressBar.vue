<template>
  <ProgressBar v-bind="{ title: `Repetation: `, finished, toRepeatCount }">
    <template #buttons>
      <ViewLastResultButton
        v-bind="{ hasLastResult }"
        @viewLastResult="$emit('viewLastResult')"
      />
      <PauseRepeatButton v-bind="{ noteId, linkId, allowPause, btn }" />
    </template>
    <template #default v-if="$slots.default">
      <slot />
    </template>
  </ProgressBar>
</template>

<script>
import PauseRepeatButton from "./PauseRepeatButton.vue";
import ViewLastResultButton from "./ViewLastResultButton.vue";
import ProgressBar from "../commons/ProgressBar.vue";

export default {
  components: { PauseRepeatButton, ViewLastResultButton, ProgressBar },
  props: {
    noteId: [String, Number],
    linkId: [String, Number],
    allowPause: { type: Boolean, default: true },
    finished: Number,
    toRepeatCount: Number,
    btn: { type: String, default: "pause" },
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

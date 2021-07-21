<template>
<div class="review-info-bar">

  <StopRepeatButton />
  <ViewLastResultButton v-bind="{lastResult}" @viewLastResult="$emit('viewLastResult')"/>
  <PauseRepeatButton v-bind="{noteId, linkId, allowPause, btn}"/>
  <div class="review-info-bar-right">
    <span :class="`progress-bar ${!!$slots.default ? 'thin' : ''}`" v-if="toRepeatCount !== null">
      <span class="progress" :style="`width: ${finished * 100 / (finished + toRepeatCount)}%`">
      </span>
      <span class="progress-text">
      {{finished}}/{{finished + toRepeatCount}}
      </span>
    </span>
    <slot />
  </div>
</div>
</template>
<script>
import StopRepeatButton from "./StopRepeatButton.vue"
import PauseRepeatButton from "./PauseRepeatButton.vue"
import ViewLastResultButton from "./ViewLastResultButton.vue"

export default {
  components: { StopRepeatButton, PauseRepeatButton, ViewLastResultButton },
  props: {
    noteId: Number,
    linkId: Number,
    allowPause: { type: Boolean, default: true },
    finished: Number,
    toRepeatCount: Number,
    btn: {type: String, default: "pause"},
    lastResult: Object,
    },
  emits: ['viewLastResult'],
  methods: {
  },
}
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
  position:absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
}

</style>
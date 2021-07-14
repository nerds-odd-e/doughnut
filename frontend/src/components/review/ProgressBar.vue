<template>
<div class="review-info-bar">

  <StopRepeatButton />
  <PauseRepeatButton v-bind="{noteId, linkId, allowPause}"/>
  <div class="review-info-bar-right">
    <span class="progress-bar" v-if="toRepeatCount !== null">
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

export default {
  components: { StopRepeatButton, PauseRepeatButton },
  props: {noteId: Number, linkId: Number, allowPause: { type: Boolean, default: true }, finished: Number, toRepeatCount: Number},
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
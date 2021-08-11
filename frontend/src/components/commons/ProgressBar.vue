<template>
<div class="review-info-bar">
  <StopRepeatButton />
  <slot name="buttons"/>
  <div class="review-info-bar-right">
    <span :class="`progress-bar ${!!$slots.default ? 'thin' : ''}`" v-if="toRepeatCount !== null">
      <span class="progress" :style="`width: ${finished * 100 / (finished + toRepeatCount)}%`">
      </span>
      <span class="progress-text">
        {{title}}{{finished}}/{{finished + toRepeatCount}}
      </span>
    </span>
    <slot />
  </div>
</div>
</template>
<script>
import StopRepeatButton from "../review/StopRepeatButton.vue"

export default {
  components: { StopRepeatButton },
  props: {
    finished: Number,
    toRepeatCount: Number,
    title: String,
    },
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
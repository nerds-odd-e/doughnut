<template>
  <ProgressBar v-bind="{ title: `Repetition: `, finished, toRepeatCount }">
    <template #buttons>
      <div class="btn-group">
        <template v-if="previousResultCursor !== undefined">
          <button
            class="btn large-btn"
            title="view previous result"
            :disabled="finished === 0 || previousResultCursor === 0"
            @click="
              $emit(
                'viewLastResult',
                previousResultCursor === undefind
                  ? finished - 1
                  : previousResultCursor - 1
              )
            "
          >
            <SvgBackward />
          </button>

          <button
            class="btn large-btn"
            title="view next result"
            :disabled="previousResultCursor >= finished - 1"
            @click="$emit('viewLastResult', previousResultCursor + 1)"
          >
            <SvgForward />
          </button>

          <button
            class="btn large-btn"
            title="Go back to review"
            @click="$emit('viewLastResult', undefined)"
          >
            <SvgResume />
          </button>
        </template>
        <button
          v-else
          class="btn large-btn"
          title="view last result"
          :disabled="finished === 0"
          @click="$emit('viewLastResult', finished - 1)"
        >
          <SvgPause />
        </button>
      </div>
    </template>
  </ProgressBar>
</template>

<script>
import ProgressBar from "../commons/ProgressBar.vue";
import SvgResume from "../svgs/SvgResume.vue";
import SvgPause from "../svgs/SvgPause.vue";
import SvgBackward from "../svgs/SvgBackward.vue";
import SvgForward from "../svgs/SvgForward.vue";

export default {
  components: {
    ProgressBar,
    SvgResume,
    SvgPause,
    SvgBackward,
    SvgForward,
  },
  props: {
    finished: Number,
    toRepeatCount: Number,
    previousResultCursor: Number,
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

.large-btn {
  svg {
    width: 30px;
    height: 30px;
  }
  &:disabled {
    opacity: 0.5;
  }
}
</style>

<template>
  <div class="header" :class="previousAnsweredQuestionCursor ? 'repeat-paused' : ''">
    <ProgressBar
      v-bind="{ title: `Recalling: `, finished, toRepeatCount }"
      @resume="$emit('viewLastAnsweredQuestion', undefined)"
      @showMore="$emit('showMore')"
    >
      <template #buttons>
        <div class="btn-group">
          <template v-if="previousAnsweredQuestionCursor !== undefined">
            <button
              class="btn large-btn"
              title="view previous answered question"
              :disabled="finished === 0 || previousAnsweredQuestionCursor === 0"
              @click="
                $emit(
                  'viewLastAnsweredQuestion',
                  !previousAnsweredQuestionCursor
                    ? finished - 1
                    : previousAnsweredQuestionCursor - 1
                )
              "
            >
              <SvgBackward />
            </button>

            <button
              class="btn large-btn"
              title="view next answered question"
              @click="$emit('viewLastAnsweredQuestion', undefined)"
            >
              <SvgResume />
            </button>
          </template>
          <button
            v-else
            class="btn large-btn"
            title="view last answered question"
            :disabled="finished === 0"
            @click="$emit('viewLastAnsweredQuestion', finished - 1)"
          >
            <SvgPause />
          </button>
          <button
            v-if="canMoveToEnd"
            class="btn large-btn"
            title="Move to end of list"
            @click="$emit('moveToEnd', currentIndex)"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="32"
              height="32"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <polyline points="19 12 12 19 5 12"></polyline>
            </svg>
          </button>
        </div>
      </template>
    </ProgressBar>
  </div>
</template>

<script setup lang="ts">
import ProgressBar from "../commons/ProgressBar.vue"
import SvgPause from "../svgs/SvgPause.vue"
import SvgBackward from "../svgs/SvgBackward.vue"
import SvgResume from "../svgs/SvgResume.vue"

defineProps({
  finished: { type: Number, required: true },
  toRepeatCount: { type: Number, required: true },
  previousAnsweredQuestionCursor: Number,
  canMoveToEnd: { type: Boolean, required: true },
  currentIndex: { type: Number, required: true },
})
defineEmits(["viewLastAnsweredQuestion", "showMore", "moveToEnd"])
</script>

<style lang="scss" scoped>
.large-btn {
  padding: 0.75rem 1rem;
  min-height: 2.5rem;
  svg {
    width: 32px;
    height: 32px;
  }
  &:disabled {
    opacity: 0.5;
  }
}
.repeat-paused {
  background-color: rgba(50, 150, 50, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>

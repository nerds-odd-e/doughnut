<template>
  <ProgressBar
    v-bind="{ title: `Recalling: `, finished, toRepeatCount }"
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
          v-if="canMoveToEnd && previousAnsweredQuestionCursor === undefined"
          class="btn large-btn"
          title="Move to end of list"
          aria-label="Move to end of list"
          @click="$emit('moveToEnd', currentIndex)"
        >
          <SvgSkip />
        </button>
      </div>
    </template>
  </ProgressBar>
</template>

<script setup lang="ts">
import ProgressBar from "../commons/ProgressBar.vue"
import SvgPause from "../svgs/SvgPause.vue"
import SvgBackward from "../svgs/SvgBackward.vue"
import SvgSkip from "../svgs/SvgSkip.vue"

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
</style>

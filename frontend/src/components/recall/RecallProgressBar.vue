<template>
  <ProgressBar
    v-bind="{ title: `Recalling: `, finished, toRepeatCount }"
    @showSettings="showSettings = !showSettings"
  >
    <template #buttons>
      <div class="btn-group-wrapper daisy-relative" style="overflow: visible;">
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
        </div>
        <RecallSessionOptionsDialog
          v-if="showSettings"
          v-bind="{
            canMoveToEnd,
            previousAnsweredQuestionCursor,
            currentIndex,
            finished,
            toRepeatCount,
            totalAssimilatedCount,
          }"
          @close-dialog="showSettings = false"
          @move-to-end="handleMoveToEnd"
          @treadmill-mode-changed="$emit('treadmill-mode-changed')"
        />
      </div>
    </template>
    <template #cogIcon>
      <SvgCog />
    </template>
  </ProgressBar>
</template>

<script setup lang="ts">
import { ref } from "vue"
import ProgressBar from "../commons/ProgressBar.vue"
import SvgPause from "../svgs/SvgPause.vue"
import SvgBackward from "../svgs/SvgBackward.vue"
import SvgCog from "../svgs/SvgCog.vue"
import RecallSessionOptionsDialog from "./RecallSessionOptionsDialog.vue"

const props = defineProps({
  finished: { type: Number, required: true },
  toRepeatCount: { type: Number, required: true },
  previousAnsweredQuestionCursor: Number,
  canMoveToEnd: { type: Boolean, required: true },
  currentIndex: { type: Number, required: true },
  totalAssimilatedCount: { type: Number, default: 0 },
})

const emit = defineEmits<{
  (e: "viewLastAnsweredQuestion", cursor: number): void
  (e: "moveToEnd", index: number): void
  (e: "treadmill-mode-changed"): void
}>()

const showSettings = ref(false)

const handleMoveToEnd = (index: number) => {
  emit("moveToEnd", index)
}
</script>

<style lang="scss" scoped>
.btn-group-wrapper {
  display: flex;
  flex-direction: column;
}

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

<template>
  <ProgressBar
    v-bind="{ title: `Recalling: `, finished, toRepeatCount }"
    @showMore="$emit('showMore')"
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
          <button
            ref="settingsButtonRef"
            class="btn large-btn"
            :class="{ 'daisy-btn-active': showSettings }"
            title="Recall settings"
            @click="showSettings = !showSettings"
          >
            <SvgCog />
          </button>
        </div>
        <Teleport to="body" v-if="showSettings">
          <RecallSettingsDialog
            :button-element="settingsButtonRef"
            v-bind="{
              canMoveToEnd,
              previousAnsweredQuestionCursor,
              currentIndex,
            }"
            @close-dialog="showSettings = false"
            @move-to-end="handleMoveToEnd"
            @treadmill-mode-changed="$emit('treadmill-mode-changed')"
          />
        </Teleport>
      </div>
    </template>
  </ProgressBar>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { Teleport } from "vue"
import ProgressBar from "../commons/ProgressBar.vue"
import SvgPause from "../svgs/SvgPause.vue"
import SvgBackward from "../svgs/SvgBackward.vue"
import SvgCog from "../svgs/SvgCog.vue"
import RecallSettingsDialog from "./RecallSettingsDialog.vue"

const props = defineProps({
  finished: { type: Number, required: true },
  toRepeatCount: { type: Number, required: true },
  previousAnsweredQuestionCursor: Number,
  canMoveToEnd: { type: Boolean, required: true },
  currentIndex: { type: Number, required: true },
})

const emit = defineEmits<{
  (e: "viewLastAnsweredQuestion", cursor: number): void
  (e: "showMore"): void
  (e: "moveToEnd", index: number): void
  (e: "treadmill-mode-changed"): void
}>()

const showSettings = ref(false)
const settingsButtonRef = ref<HTMLButtonElement | null>(null)

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

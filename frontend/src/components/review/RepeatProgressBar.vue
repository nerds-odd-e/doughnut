<template>
  <div class="header" :class="previousResultCursor ? 'repeat-paused' : ''">
    <ProgressBar
      v-bind="{ title: `Repetition: `, finished, toRepeatCount, paused }"
      @resume="$emit('viewLastResult', undefined)"
    >
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
                  !previousResultCursor
                    ? finished - 1
                    : previousResultCursor - 1,
                )
              "
            >
              <SvgBackward />
            </button>

            <button
              class="btn large-btn"
              title="view next result"
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
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import { useRouter } from "vue-router"
import ProgressBar from "../commons/ProgressBar.vue"
import SvgPause from "../svgs/SvgPause.vue"
import SvgBackward from "../svgs/SvgBackward.vue"
import SvgResume from "../svgs/SvgResume.vue"

defineProps({
  finished: { type: Number, required: true },
  toRepeatCount: { type: Number, required: true },
  previousResultCursor: Number,
})
defineEmits(["viewLastResult"])

const router = useRouter()

const paused = computed(() => {
  const routeName = router.currentRoute.value.name
  return routeName !== "repeat" && routeName !== "repeat-answer"
})
</script>

<style lang="scss" scoped>
.large-btn {
  svg {
    width: 30px;
    height: 30px;
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

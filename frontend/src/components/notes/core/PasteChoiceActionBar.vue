<template>
  <div
    v-if="pasteChoice"
    ref="barRef"
    class="paste-choice fixed z-40 flex items-center gap-1 text-sm"
    :style="positionStyle"
    data-testid="paste-choice"
    @mouseenter="$emit('pause')"
    @mouseleave="$emit('resume')"
    @focusin="$emit('pause')"
    @focusout="$emit('resume')"
  >
    <button
      type="button"
      class="daisy-btn daisy-btn-ghost min-h-[2.75rem] min-w-[2.75rem]"
      data-testid="paste-choice-action"
      @mousedown.prevent
      @click="pasteChoice.replace()"
    >
      Use original text
    </button>
    <button
      type="button"
      class="daisy-btn daisy-btn-ghost daisy-btn-square min-h-[2.75rem] min-w-[2.75rem]"
      data-testid="paste-choice-dismiss"
      aria-label="Dismiss"
      @mousedown.prevent
      @click="$emit('dismiss')"
    >
      <X :size="14" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { X } from "@lucide/vue"
import { computed, ref, type PropType } from "vue"
import type { PasteChoice } from "@/composables/useNoteContentPaste"
import { usePasteChoicePosition } from "@/composables/usePasteChoicePosition"

const props = defineProps({
  pasteChoice: { type: Object as PropType<PasteChoice | null>, default: null },
})

defineEmits<{ pause: []; resume: []; dismiss: [] }>()

const barRef = ref<HTMLElement | null>(null)
const { positionStyle } = usePasteChoicePosition(
  computed(() => props.pasteChoice?.anchorRect),
  barRef
)
</script>

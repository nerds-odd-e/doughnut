<template>
  <dialog
    ref="dialogRef"
    class="daisy-modal"
    :class="{ 'daisy-modal-open': open }"
    data-testid="new-block-title-dialog"
    @close="emit('cancel')"
  >
    <div class="daisy-modal-box">
      <h2 class="text-lg font-semibold">Name the new block</h2>
      <input
        v-if="open"
        v-model="titleInput"
        data-testid="new-block-title-input"
        class="daisy-input w-full mt-2"
        type="text"
      />
      <div class="daisy-modal-action">
        <button
          type="button"
          class="daisy-btn daisy-btn-primary"
          data-testid="new-block-title-confirm"
          @click="onConfirm"
        >
          Confirm
        </button>
        <button type="button" class="daisy-btn" @click="emit('cancel')">
          Cancel
        </button>
      </div>
    </div>
    <form method="dialog" class="daisy-modal-backdrop">
      <button type="button" @click="emit('cancel')">close</button>
    </form>
  </dialog>
</template>

<script setup lang="ts">
import { useDaisyDialog } from "@/composables/useDaisyDialog"
import { ref, toRef, watch } from "vue"

const props = defineProps<{
  open: boolean
  defaultTitle?: string
}>()

const emit = defineEmits<{
  confirm: [title: string | undefined]
  cancel: []
}>()

const titleInput = ref(props.defaultTitle ?? "")
const dialogRef = ref<HTMLDialogElement | null>(null)
useDaisyDialog(toRef(props, "open"), dialogRef)

watch(
  () => props.defaultTitle,
  (v) => {
    titleInput.value = v ?? ""
  }
)

function onConfirm() {
  emit("confirm", titleInput.value || undefined)
}
</script>

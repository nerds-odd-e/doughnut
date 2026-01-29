<template>
  <RadioButtons
    scope-name="review_setting"
    field="level"
    :model-value="levelAsString"
    :error-message="errors.level"
    :options="levelOptions"
    @update:model-value="updateLevel"
  />

  <div class="daisy-mb-4">
    <label class="daisy-label">
      <span class="daisy-label-text">Custom AI Instruction for Recall Questions</span>
    </label>
    <textarea
      v-model="aiInstructionValue"
      class="daisy-textarea daisy-textarea-bordered daisy-w-full"
      rows="3"
      placeholder="Please enter your custom instruction for recall question"
      @blur="updateAiInstruction"
    />
  </div>

  <CheckInput
    scope-name="ai_assistant"
    field="applyToChildren"
    :model-value="applyToChildrenValue"
    @update:model-value="updateApplyToChildren"
  />

  <CheckInput
    v-if="!isLinkNote"
    scope-name="review_setting"
    field="rememberSpelling"
    :model-value="rememberSpellingValue"
    :error-message="spellingDisabledMessage"
    :disabled="isSpellingDisabled"
    @update:model-value="updateModelValue({ rememberSpelling: $event })"
  />
  <CheckInput
    scope-name="review_setting"
    field="skipMemoryTracking"
    :model-value="formData.skipMemoryTracking"
    :error-message="errors.skipMemoryTracking"
    @update:model-value="updateModelValue({ skipMemoryTracking: $event })"
  />
</template>

<script lang="ts">
import type { NoteRecallSetting, NoteAiAssistant } from "@generated/backend"
import { NoteController } from "@generated/backend/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { PropType } from "vue"
import { defineComponent, computed, ref } from "vue"
import CheckInput from "../form/CheckInput.vue"
import RadioButtons from "../form/RadioButtons.vue"

export default defineComponent({
  components: { CheckInput, RadioButtons },
  props: {
    noteId: { type: Number, required: true },
    noteRecallSetting: {
      type: Object as PropType<NoteRecallSetting>,
      required: false,
    },
    noteDetails: {
      type: String,
      required: false,
    },
    isLinkNote: {
      type: Boolean,
      required: true,
    },
    noteAiAssistant: {
      type: Object as PropType<NoteAiAssistant>,
      required: false,
    },
  },
  emits: ["levelChanged", "rememberSpellingChanged"],
  setup(props, { emit }) {
    const formData = ref<NoteRecallSetting>(props.noteRecallSetting || {})
    const errors = ref<Partial<Record<keyof NoteRecallSetting, string>>>({})

    // AI Assistant state
    const aiInstructionValue = ref(
      props.noteAiAssistant?.additionalInstructionsToAi || ""
    )
    const applyToChildrenValue = ref(
      props.noteAiAssistant?.applyToChildren || false
    )

    const isSpellingDisabled = computed(
      () => !props.noteDetails || props.noteDetails.trim() === ""
    )

    const spellingDisabledMessage = computed(() =>
      isSpellingDisabled.value
        ? "Remember spelling note need to have detail"
        : errors.value.rememberSpelling
    )

    const rememberSpellingValue = computed(
      () => !isSpellingDisabled.value && formData.value.rememberSpelling
    )

    const levelAsString = computed(() =>
      formData.value.level !== undefined
        ? formData.value.level.toString()
        : undefined
    )

    const levelOptions = [0, 1, 2, 3, 4, 5, 6].map((level) => ({
      value: level.toString(),
      label: level.toString(),
    }))

    const updateModelValue = async (newValue: Partial<NoteRecallSetting>) => {
      formData.value = {
        ...formData.value,
        ...newValue,
      }
      const { error } = await apiCallWithLoading(() =>
        NoteController.updateNoteRecallSetting({
          path: { note: props.noteId },
          body: formData.value,
        })
      )
      if (!error) {
        if (newValue.level !== undefined) {
          emit("levelChanged", newValue.level)
        }
        if (newValue.rememberSpelling !== undefined) {
          emit("rememberSpellingChanged", newValue.rememberSpelling)
        }
      } else {
        // Error is handled by global interceptor (toast notification)
        // Extract field-level errors if available (for 400 validation errors)
        const errorObj = toOpenApiError(error)
        errors.value = errorObj.errors || {}
      }
    }

    const updateLevel = (value: string) => {
      updateModelValue({ level: Number.parseInt(value) })
    }

    const updateAiInstruction = async () => {
      const { error } = await apiCallWithLoading(() =>
        NoteController.updateNoteAiAssistant({
          path: { note: props.noteId },
          body: {
            id: 0,
            additionalInstructionsToAi: aiInstructionValue.value,
            applyToChildren: applyToChildrenValue.value,
          },
        })
      )
      if (error) {
        console.error("Failed to update AI assistant settings:", error)
      }
    }

    const updateApplyToChildren = async (value: boolean) => {
      applyToChildrenValue.value = value
      await updateAiInstruction()
    }

    return {
      formData,
      errors,
      isSpellingDisabled,
      spellingDisabledMessage,
      rememberSpellingValue,
      levelAsString,
      levelOptions,
      updateModelValue,
      updateLevel,
      aiInstructionValue,
      applyToChildrenValue,
      updateAiInstruction,
      updateApplyToChildren,
    }
  },
})
</script>

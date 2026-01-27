<template>
  <RadioButtons
    scope-name="review_setting"
    field="level"
    :model-value="levelAsString"
    :error-message="errors.level"
    :options="levelOptions"
    @update:model-value="updateLevel"
  />
  <CheckInput
    scope-name="review_setting"
    field="rememberSpelling"
    :model-value="formData.rememberSpelling"
    :error-message="errors.rememberSpelling"
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
import type { NoteRecallSetting } from "@generated/backend"
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
  },
  emits: ["levelChanged"],
  setup(props, { emit }) {
    const formData = ref<NoteRecallSetting>(props.noteRecallSetting || {})
    const errors = ref<Partial<Record<keyof NoteRecallSetting, string>>>({})

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

    return {
      formData,
      errors,
      levelAsString,
      levelOptions,
      updateModelValue,
      updateLevel,
    }
  },
})
</script>

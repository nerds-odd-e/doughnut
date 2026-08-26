<template>
  <RadioButtons
    scope-name="recall_setting"
    field="level"
    :model-value="levelAsString"
    :error-message="errors.level"
    :options="levelOptions"
    @update:model-value="updateLevel"
  />
</template>

<script lang="ts">
import type { NoteRecallSetting } from "@generated/donut-backend-api"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { PropType } from "vue"
import { defineComponent, computed, ref, watch } from "vue"
import RadioButtons from "../form/RadioButtons.vue"

export default defineComponent({
  components: { RadioButtons },
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

    // Keep formData in sync with props for merge operations in updateModelValue
    watch(
      () => props.noteRecallSetting,
      (newValue) => {
        if (newValue) {
          formData.value = newValue
        }
      }
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

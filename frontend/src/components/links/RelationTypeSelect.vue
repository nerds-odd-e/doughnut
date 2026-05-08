<template>
  <RadioButtons
    :model-value="selectedRadioValue"
    @update:model-value="onRadioSelection($event)"
    :scope-name="scopeName"
    :field="field"
    :options="optionsWithCustom"
    :error-message="errorMessage"
  >
    <template #labelAddition="{ value }">
      <div v-if="value !== CUSTOM_SENTINEL" class="daisy-text-center">
        <SvgRelationTypeIcon
          :relation-type="value as RelationTypeLabel"
          :inverse-icon="inverseIcon"
        />
      </div>
    </template>
  </RadioButtons>
  <label
    v-if="selectedRadioValue === CUSTOM_SENTINEL"
    class="daisy-flex daisy-flex-col daisy-gap-1 daisy-mt-2 daisy-text-sm"
    :for="customInputId"
  >
    <span class="daisy-font-medium">Custom relationship</span>
    <div class="daisy-join daisy-join-horizontal daisy-w-full">
      <input
        :id="customInputId"
        v-model="customText"
        type="text"
        class="daisy-input daisy-input-bordered daisy-input-sm daisy-join-item daisy-min-w-0 daisy-flex-1"
        autocomplete="off"
        @keydown.enter.prevent="emitCustomCommitted"
      />
      <button
        type="button"
        class="daisy-btn daisy-btn-outline daisy-btn-sm daisy-join-item daisy-shrink-0"
        title="Apply custom relationship"
        aria-label="Apply custom relationship"
        @mousedown.prevent
        @click="emitCustomCommitted"
      >
        <CornerDownLeft class="daisy-w-4 daisy-h-4" />
      </button>
    </div>
  </label>
</template>

<script setup lang="ts">
import { computed, ref, watch, useId } from "vue"
import {
  CUSTOM_RELATION_RADIO_SENTINEL,
  relationTypeOptions,
  type RelationTypeLabel,
} from "../../models/relationTypeOptions"
import { CornerDownLeft } from "lucide-vue-next"
import RadioButtons from "../form/RadioButtons.vue"
import SvgRelationTypeIcon from "../svgs/SvgRelationTypeIcon.vue"

const CUSTOM_SENTINEL = CUSTOM_RELATION_RADIO_SENTINEL

const props = defineProps({
  scopeName: String,
  modelValue: {
    type: String,
    required: false,
    default: undefined,
  },
  errorMessage: String,
  allowEmpty: { type: Boolean, default: false },
  field: { type: String, default: "relationType" },
  inverseIcon: Boolean,
})

const emit = defineEmits<{
  "update:modelValue": [value: string]
}>()

const customInputId = useId()

function isKnownPredefinedLabel(value: string | undefined): boolean {
  if (!value?.trim()) return false
  return relationTypeOptions.some(({ label }) => label === value)
}

/** User picked a radio before parent echoed it (e.g. Custom…) */
const internalRadioPick = ref<string | undefined>(undefined)

const optionsWithCustom = computed(() => {
  const predefined = relationTypeOptions.map(({ label }) => ({
    value: label as string,
    label: label as string,
  }))
  return [
    ...predefined,
    {
      value: CUSTOM_SENTINEL,
      label: "Custom…",
    },
  ]
})

function radioFromModel(model: string | undefined): string | undefined {
  const v = model
  if (v === undefined || v === "") return undefined
  if (isKnownPredefinedLabel(v)) return v
  return CUSTOM_SENTINEL
}

const selectedRadioValue = computed(() => {
  if (internalRadioPick.value !== undefined) return internalRadioPick.value
  return radioFromModel(props.modelValue as string | undefined)
})

const customText = ref("")

watch(
  () => props.modelValue as string | undefined,
  (v) => {
    internalRadioPick.value = undefined
    if (v === undefined || v === "") {
      customText.value = ""
      return
    }
    if (isKnownPredefinedLabel(v)) {
      customText.value = ""
      return
    }
    customText.value = v
  },
  { immediate: true }
)

function onRadioSelection(val: string) {
  if (val === CUSTOM_SENTINEL) {
    internalRadioPick.value = CUSTOM_SENTINEL
    if (isKnownPredefinedLabel(props.modelValue as string | undefined)) {
      customText.value = ""
    } else if (props.modelValue) {
      customText.value = props.modelValue as string
    }
    return
  }
  internalRadioPick.value = undefined
  customText.value = ""
  emit("update:modelValue", val)
}

function emitCustomCommitted() {
  if (selectedRadioValue.value !== CUSTOM_SENTINEL) return
  internalRadioPick.value = undefined
  emit("update:modelValue", customText.value.trim())
}
</script>

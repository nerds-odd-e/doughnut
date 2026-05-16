<template>
  <RadioButtons
    :model-value="selectedRadioValue"
    variant="chips"
    radiogroup-class="max-h-[min(50vh,18rem)] overflow-y-auto overflow-x-hidden overscroll-y-contain pe-1"
    @update:model-value="onRadioSelection($event)"
    :scope-name="scopeName"
    :field="field"
    :options="optionsWithCustom"
    :error-message="errorMessage"
  >
    <template #labelAddition="{ value }">
      <PenLine
        v-if="value === CUSTOM_SENTINEL"
        class="h-4 w-4 shrink-0 opacity-80"
        aria-hidden="true"
      />
      <div
        v-else
        class="flex shrink-0 items-center"
      >
        <SvgRelationTypeIcon
          width="32px"
          height="16px"
          :relation-type="value as RelationTypeLabel"
          :inverse-icon="inverseIcon"
        />
      </div>
    </template>
  </RadioButtons>
  <div
    v-if="selectedRadioValue === CUSTOM_SENTINEL"
    class="mt-3 rounded-box border border-base-content/15 bg-base-200/40 p-3"
  >
    <label
      class="daisy-label mb-2 block p-0 text-sm font-medium"
      :for="customInputId"
    >
      Custom relationship
    </label>
    <div class="daisy-join daisy-join-horizontal w-full">
      <input
        :id="customInputId"
        v-model="customText"
        type="text"
        class="daisy-input daisy-input-sm daisy-join-item min-w-0 flex-1"
        placeholder="Type any phrase (e.g. inspired by)"
        autocomplete="off"
        @keydown.enter.prevent="emitCustomCommitted"
      />
      <button
        type="button"
        class="daisy-btn daisy-btn-primary daisy-btn-sm daisy-join-item shrink-0"
        title="Apply custom relationship"
        aria-label="Apply custom relationship"
        @mousedown.prevent
        @click="emitCustomCommitted"
      >
        <CornerDownLeft class="w-4 h-4" />
      </button>
    </div>
    <p class="mb-0 mt-2 text-xs leading-snug text-base-content/60">
      Confirm with Enter or the apply button (same as choosing a preset).
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, useId } from "vue"
import {
  CUSTOM_RELATION_RADIO_SENTINEL,
  relationTypeOptions,
  type RelationTypeLabel,
} from "../../models/relationTypeOptions"
import { CornerDownLeft, PenLine } from "lucide-vue-next"
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

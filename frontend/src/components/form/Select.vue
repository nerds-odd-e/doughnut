<template>
  <InputWithType v-bind="{ scopeName, field, errors }">
    <select
      :class="`select-control form-control ${!!errors ? 'is-invalid' : ''}`"
      :id="`${scopeName}-${field}`"
      :name="scopeName"
      v-model="localValue"
    >
      <option
        class="options"
        v-for="option in options"
        :key="option"
        :value="option"
      >
        {{ option }}
      </option>
    </select>
  </InputWithType>
</template>

<script lang="ts" setup>
import { PropType, ref, watch } from "vue";
import InputWithType from "./InputWithType.vue";

const props = defineProps({
  modelValue: String,
  scopeName: String,
  field: String,
  options: Array as PropType<string[]>,
  errors: Object,
});

const emit = defineEmits(["update:modelValue"]);

const localValue = ref(props.modelValue);

watch(localValue, (newValue) => {
  emit("update:modelValue", newValue);
});
</script>

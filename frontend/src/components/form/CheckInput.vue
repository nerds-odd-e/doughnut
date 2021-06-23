<template>
<div class="form-check">
    <input
            type="checkbox"
            class="form-check-input"
            :id="`${scopeName}-${field}`"
            :name="field"
            :checked="modelValue"
            @change="$emit('update:modelValue', $event.target.checked)"
    />
    <label class="form-check-label" :for="`${scopeName}-${field}`">{{startCase(camelCase(field))}}</label>
</div>

</template>

<script setup>
import { startCase, camelCase } from "lodash"

const props = defineProps({modelValue: String, scopeName: String, field: String, errors: Object})
const emit = defineEmit(['update:modelValue'])

</script>

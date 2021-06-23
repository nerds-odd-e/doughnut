<template>
<div class="form-check">
    <input
            type="checkbox"
            class="form-check-input"
            :id="`${scopeName}-${field}`"
            :name="field"
            v-model="modelValue"
    />
    <label class="form-check-label" :for="`${scopeName}-${field}`">{{startCase(camelCase(field))}}</label>
</div>

</template>

<script>
import { startCase, camelCase } from "lodash"

    export  default {
    props: {modelValue: String, scopeName: String, field: String, errors: Object},
    emits: ['update:modelValue'],
    methods: {startCase, camelCase}
    }
  

</script>

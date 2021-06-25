<template>
<div class="form-group">
    <slot v-if="beforeLabel" />
    <label :for="`${scopeName}-${field}`">{{ startCase(camelCase( field))}}</label>
    <slot v-if="!beforeLabel" />
    <div class="invalid-feedback" v-if="!!errors">{{errors}}</div>
</div>
</template>

<script setup>
  import {startCase, camelCase } from "lodash"
  const props = defineProps({ scopeName: String, field: String, errors: Object, beforeLabel: {type: Boolean, default: false}})
</script>

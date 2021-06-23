<template>
<div class="form-group">
    <label :for="`${scopeName}-${field}`">{{ startCase(camelCase( field))}}</label>
    <slot />
    <div class="invalid-feedback" v-if="false" cond="'${#fields.hasErrors(field)}'" th:errors="*{__${field}__}"/>
</div>
</template>

<script setup>
  import {startCase, camelCase } from "lodash"
  const props = defineProps({ scopeName: String, field: String, errors: Object})
</script>

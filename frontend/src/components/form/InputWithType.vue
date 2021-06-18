<template>
<div class="form-group">
    <label :for="`${scopeName}-${field}`">{{field.toTitleCase()}}</label>
    <slot />
    <div class="invalid-feedback" v-if="false" cond="'${#fields.hasErrors(field)}'" th:errors="*{__${field}__}"/>
</div>
</template>

<script setup>
  const props = defineProps({ scopeName: String, field: String, errors: Object})

  String.prototype.toTitleCase = function () {
    return this.replace(/\w\S*/g, function(txt){return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();});
  };
</script>

<template>
    <CheckInput scopeName='review_setting' field='rememberSpelling' v-model="modelValue.rememberSpelling"/>
</template>

<script setup>
import CheckInput from "../form/CheckInput.vue"
  const props = defineProps({ modelValue: Object})
</script>

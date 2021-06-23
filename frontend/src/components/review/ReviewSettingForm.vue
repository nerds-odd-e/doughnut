<template>
    <CheckInput scopeName='review_setting' field='rememberSpelling' v-model="modelValue.rememberSpelling"/>
</template>

<script setup>
  import { watch } from "@vue/runtime-core"
import CheckInput from "../form/CheckInput.vue"
  const props = defineProps({ modelValue: Object})
  const emit = defineEmit(['update:modelValue'])
  watch(()=>({...props.modelValue.value}),
    ()=> emit("update:modelValue", props.modelValue.value)
  )
</script>

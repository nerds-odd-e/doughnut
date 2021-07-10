<template>
    <CheckInput scopeName='review_setting' field='rememberSpelling' v-model="modelValue.rememberSpelling" :errors="errors.rememberSpelling"/>
</template>

<script>
import CheckInput from "../form/CheckInput.vue"
export default {
  props: { modelValue: Object, errors: Object},
  components: { CheckInput }
}
</script>

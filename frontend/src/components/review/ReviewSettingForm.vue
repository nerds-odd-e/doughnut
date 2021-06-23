<template>
    <CheckInput scopeName='review_setting' field='rememberSpelling' v-model="reviewSetting.rememberSpelling"/>
</template>

<script setup>
  import CheckInput from "../form/CheckInput.vue"
  const props = defineProps({ reviewSetting: Object})
</script>

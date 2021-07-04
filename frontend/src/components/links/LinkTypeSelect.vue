<template>
  <Select v-if="!!$staticInfo"
    scopeName='link'
    field='typeId'
    v-model="modelValue"
    :options="$staticInfo.linkTypeOptions"
    :errors="errors"
    @update:modelValue="$emit('update:modelValue', $event)"
   />
</template>

<script>
import Select from "../form/Select.vue"

export default {
  name: 'LinkTypeSelect',
  props: { scopeName: String, modelValue: Object, errors: Object },
  components: {Select},
  emits: ['update:modelValue'],
}

</script>

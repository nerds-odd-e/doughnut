<template>
  <Select v-if="!!$staticInfo"
    v-model="modelValue"
    @update:modelValue="$emit('update:modelValue', $event)"
    v-bind="{scopeName, field, options, errors}"
   />
</template>

<script>
import Select from "../form/Select.vue"

export default {
  name: 'LinkTypeSelect',
  props: { scopeName: String, modelValue: Object, errors: Object, allowEmpty: {type: Boolean, default: false}, field: {type: String, defalt: 'linkType'} },
  components: {Select},
  emits: ['update:modelValue'],
  computed: {
    options() {
      if(!!this.allowEmpty) {
        return [{value: '', label: 'Default'}, ...this.$staticInfo.linkTypeOptions]
      }
      return this.$staticInfo.linkTypeOptions
    }
  }
}

</script>

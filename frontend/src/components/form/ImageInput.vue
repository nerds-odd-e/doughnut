<template>
  <InputWithType v-bind="{scopeName, field, errors}">
      <input
              class="text-input-control form-control put_error_class_here"
              :id="`${scopeName}-${field}`"
              type="file"
              :name="field"
              @change="update($event.target.files[0])"
              :placeholder="placeholder"
              :autofocus="autofocus"
              autocomplete="off"
              autocapitalize="off"
      />
  </InputWithType>
</template>

<script>
import InputWithType from "./InputWithType.vue"
export default {
  props: {
    modelValue: String,
     scopeName: String,
      field: String,
      placeholder: {type: String, default: null},
      autofocus: {type: Boolean, default: false},
      errors: Object
      },
  emits: ['update:modelValue'],
  components: {InputWithType},
  methods: {
    update(data) {
      this.$emit('update:modelValue', data)
    }
  }
}

</script>

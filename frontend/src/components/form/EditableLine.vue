<template>
  <div>
    <h2 style="display: inline-block;" @click="onClickText" v-if="!isEditing">{{ modelValue }}</h2>
    <TextInput
     v-bind="$attrs"
     :modelValue="modelValue"
     @update:modelValue="$emit('update:modelValue', $event)"
     @blur="onBlurTextField"
     v-if="isEditing"
     v-on:keyup.enter="$event.target.blur()"/>
  </div>
</template>

<script>
import TextInput from "./TextInput.vue";

export default {
  name: "EditableLine",
  props: {
    modelValue: String,
    scopeName: String,
    field: String,
    title: String,
    errors: Object,
  },
  emits: ["update:modelValue", "blur"],
  components: {
    TextInput
  },
  data() {
    return {
      isEditing: false,
    };
  },
  methods: {
    onClickText() {
      this.isEditing = true;
    },
    onBlurTextField() {
      this.isEditing = false;
      this.$emit("blur");
    }
  },
};
</script>
<template>
  <div>
    <pre :class="`note-description`" style="white-space: pre-wrap"
        @click="onClickText" v-if="!isEditing">{{
          modelValue
    }}</pre>
    <TextArea class="editor" 
      v-bind="$props"
      @update:modelValue="$emit('update:modelValue', $event)"
      @blur="onBlurTextField"
      v-if="isEditing"
      v-on:keydown.enter.shift="$event.target.blur()"/>
  </div>
</template>

<script>
import TextArea from "./TextArea.vue";

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
    TextArea
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
      this.$emit("blur", {description: this.modelValue});
    }
  },
};
</script>

<style lang="sass" scoped>
.editor
  width: 100%

</style>

<template>
  <div>
    <template v-if="!isEditing">
      <component v-bind:is="displayComponent"
        v-if="!!modelValue"
        v-text="modelValue"
        @click="onClickText"/>
      <SvgEditText v-else @click="onClickText"/>
    </template>
    <component v-bind:is="editingComponent"
     v-else
     v-focus
     class="editor" 
     :modelValue="modelValue"
     :scopeName="scopeName"
     :field="field"
     :title="title"
     :errors="errors"
     @update:modelValue="$emit('update:modelValue', $event)"
     @blur="onBlurTextField"
     v-on:keydown.enter="onEnterKey($event)"/>
  </div>
</template>

<script>
import TextArea from "./TextArea.vue";
import TextInput from "./TextInput.vue";
import SvgEditText from "../svgs/SvgEditText.vue";

export default {
  name: "EditableLine",
  props: {
    multipleLine: Boolean,
    modelValue: String,
    scopeName: String,
    field: String,
    title: String,
    errors: Object,
  },
  emits: ["update:modelValue", "blur"],
  components: {
    TextArea,
    TextInput,
    SvgEditText,
  },
  data() {
    return {
      isEditing: false,
    };
  },
  computed: {
    displayComponent() {
      return this.multipleLine ? 'pre' : 'h2'
    },
    editingComponent() {
      return this.multipleLine ? 'TextArea' : 'TextInput'
    },
  },
  methods: {
    onClickText() {
      this.isEditing = true;
    },
    onEnterKey(event) {
      if(!this.multipleLine || event.shiftKey) {
        event.target.blur()
      }
    },
    onBlurTextField() {
      this.isEditing = false;
      this.$emit("blur");
    }
  },
};
</script>

<style lang="sass" scoped>
.editor
  width: 100%

pre
 white-space: pre-wrap
</style>

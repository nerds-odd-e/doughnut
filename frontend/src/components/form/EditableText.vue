<template>
  <div class="text" @click="startEditing">
    <template v-if="!isEditing">
      <component v-bind:is="displayComponent" v-if="!!modelValue">
        {{ modelValue }}
      </component>
      <SvgEditText v-else />
    </template>
    <component
      v-bind:is="editingComponent"
      v-else
      v-focus
      class="editor"
      v-model="localValue"
      :scope-name="scopeName"
      :field="field"
      :title="title"
      :errors="errors"
      @blur="onBlurTextField"
      @keydown.enter="onEnterKey($event)"
    />
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
      initialValue: null,
      localValue: null,
      isEditing: false,
    };
  },
  computed: {
    displayComponent() {
      return this.multipleLine ? "pre" : "h2";
    },
    editingComponent() {
      return this.multipleLine ? "TextArea" : "TextInput";
    },
  },
  methods: {
    startEditing() {
      if (this.isEditing) return;
      this.initialValue = this.modelValue;
      this.localValue = this.modelValue;
      this.isEditing = true;
    },
    onEnterKey(event) {
      if (!this.multipleLine || event.shiftKey) {
        event.target.blur();
      }
    },
    onBlurTextField() {
      this.isEditing = false;
      if (this.initialValue !== this.localValue) {
        this.$emit("update:modelValue", this.localValue);
        this.$emit("blur");
      }
    },
  },
};
</script>

<style lang="sass" scoped>
.editor
  width: 100%

.text
  cursor: text

pre
 white-space: pre-wrap
</style>

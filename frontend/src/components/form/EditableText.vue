<template>
  <div class="text" @click="startEditing">
    <template v-if="!isEditing">
      <h2 v-if="!!modelValue">
        {{ modelValue }}
      </h2>
      <SvgEditText v-else />
    </template>
    <TextInput
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

<script lang="ts">
import { defineComponent } from "vue";

import TextInput from "./TextInput.vue";
import SvgEditText from "../svgs/SvgEditText.vue";

export default defineComponent({
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
    TextInput,
    SvgEditText,
  },
  data() {
    return {
      initialValue: this.modelValue as string | undefined,
      localValue: this.modelValue as string | undefined,
      isEditing: false,
    };
  },
  watch: {
    localValue() {
      this.$emit("update:modelValue", this.localValue);
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
      event.target.blur();
    },
    onBlurTextField() {
      this.isEditing = false;
      this.$emit("blur");
    },
  },
});
</script>

<style lang="sass" scoped>
.editor
  width: 100%

pre
 white-space: pre-wrap
</style>

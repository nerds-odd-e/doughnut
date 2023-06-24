<template>
  <div class="text" @click="startEditing">
    <template v-if="!isEditing">
      <InputWithType v-bind="{ scopeName, field, title, errors }">
        <h2 v-if="!!modelValue">
          {{ modelValue }}
        </h2>
        <SvgEditText v-else />
      </InputWithType>
    </template>
    <TextInput
      v-else
      v-focus
      class="editor"
      :model-value="modelValue"
      @update:model-value="$emit('update:modelValue', $event)"
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
    errors: String,
  },
  emits: ["update:modelValue", "blur"],
  components: {
    TextInput,
    SvgEditText,
  },
  data() {
    return {
      isEditing: false,
    };
  },
  methods: {
    startEditing() {
      if (this.isEditing) return;
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

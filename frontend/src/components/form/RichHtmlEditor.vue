<template>
  <QuillEditor
    ref="quillEditor"
    v-model:content="localValue"
    :options="editorOptions"
    :content-type="'html'"
    :read-only="readonly"
    @blur="onBlurTextField"
    @update:content="onUpdateContent"
    @focus="hadFocus = true"
  />
</template>

<script lang="ts">
import { QuillEditor } from "@vueup/vue-quill"
import { defineComponent } from "vue"

import "quill/dist/quill.snow.css"

export default defineComponent({
  props: {
    multipleLine: Boolean,
    modelValue: String,
    scopeName: String,
    field: String,
    title: String,
    errors: Object,
    readonly: Boolean,
  },
  emits: ["update:modelValue", "blur"],
  components: {
    QuillEditor,
  },
  data() {
    return {
      editorOptions: {
        modules: {
          toolbar: false,
          keyboard: {
            bindings: {
              custom: {
                key: 13,
                shiftKey: true,
                handler: this.onBlurTextField,
              },
            },
          },
        },
        placeholder: this.readonly ? "" : "Enter note details here...",
      },
      localValue: this.modelValue,
      hadFocus: false as boolean,
    }
  },
  watch: {
    modelValue() {
      this.localValue = this.modelValue
    },
  },
  methods: {
    onUpdateContent() {
      if (this.localValue === this.modelValue) {
        return
      }
      this.$emit("update:modelValue", this.localValue)
    },
    onBlurTextField() {
      this.$emit("blur")
    },
  },
})
</script>

<style lang="sass">
.ql-editor
  padding: 0
  margin-bottom: 15px
  &::before
    left: 0 !important
    right:0 !important
  li
    list-style-type: inherit
.ql-container.ql-snow
  border: none
  font-size: inherit !important
</style>

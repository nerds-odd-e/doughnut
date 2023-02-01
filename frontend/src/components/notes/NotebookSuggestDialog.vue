<template>
  <LoadingPage v-bind="{ contentExists: true }">
    <h1>Suggested Description</h1>
    <form @submit.prevent.once="processForm">
      <InputWithType
        v-bind="{
          scopeName,
          field,
        }"
      >
        <textarea
          :class="`area-control form-control`"
          :id="`description-input`"
          :name="field"
          :autofocus="true"
          v-model="modelValue"
          role="suggestdescription"
          autocomplete="off"
          autocapitalize="off"
          rows="8"
          ref="input"
        />
      </InputWithType>
      <input type="submit" value="Use" class="btn btn-primary" />
      <input
        type="submit"
        value="Copy to clipboard"
        class="btn btn-primary"
        @click="copyText()"
      />
    </form>
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import InputWithType from "../form/InputWithType.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    size: { type: String, default: "" },
  },
  components: {
    LoadingPage,
  },
  data() {
    return {
      noteFormData: {},
      errors: {},
      modelValue: "Placeholder",
      scopeName: "to be changed",
      field: "to be changed",
    };
  },
  computed: {
    textContent() {
      return {
        title: this.note.textContent.title,
        description: this.modelValue,
        updatedAt: this.note.textContent.updatedAt,
      };
    },
  },
  emits: ["done"],
  methods: {
    copyText() {
      const element = this.$refs.input as InstanceType<
        typeof HTMLTextAreaElement
      >;
      element.select();
      element.setSelectionRange(0, 99999);
      document.execCommand("copy");
    },
    processForm() {
      this.storageAccessor
        .api()
        .updateTextContent(
          this.note.id,
          this.textContent,
          this.note.textContent
        )
        .then(() => {
          this.$emit("done");
        });
    },
  },
});
</script>

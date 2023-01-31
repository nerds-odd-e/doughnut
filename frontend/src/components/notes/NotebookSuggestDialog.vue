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
          :id="`${scopeName}-${field}`"
          :name="field"
          :value="modelValue"
          :placeholder="placeholder"
          :autofocus="autofocus"
          autocomplete="off"
          autocapitalize="off"
          rows="8"
          ref="input"
        />
      </InputWithType>
      <input
        type="submit"
        value="Use"
        class="btn btn-primary"
        disabled="true"
      />
      <input
        type="submit"
        value="Copy to clipboard"
        class="btn btn-primary"
        @click="copyText()"
      />
    </form>
  </LoadingPage>
</template>

<script>
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import InputWithType from "../form/InputWithType.vue";

export default {
  setup() {
    return useLoadingApi();
  },
  props: {
    size: { type: String, default: "" },
  },
  components: {
    LoadingPage,
  },
  data() {
    return {
      noteFormData: {},
      errors: {},
      modelValue: "",
    };
  },
  methods: {
    copyText() {
      const element = this.$refs.input;
      element.select();
      element.setSelectionRange(0, 99999);
      document.execCommand("copy");
    },
  },
};
</script>

<template>
  <TextInput
    scope-name="wikidataID"
    field="wikidataID"
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :error-message="errorMessage"
    placeholder="example: `Q1234`"
  >
    <template #input_prepend>
      <button
        title="Wikidata Id"
        type="button"
        class="daisy-btn daisy-btn-outline daisy-btn-neutral"
        @click.prevent="openDialog"
      >
        <SvgSearchWikidata />
      </button>
    </template>
  </TextInput>
  <WikidataSearchDialog
    v-if="showDialog"
    :search-key="searchKey"
    :current-title="currentTitle"
    @close="closeDialog"
    @selected="handleSelected"
  />
</template>

<script lang="ts">
import type { WikidataSearchEntity } from "@generated/backend"
import { defineComponent } from "vue"
import TextInput from "../form/TextInput.vue"
import SvgSearchWikidata from "../svgs/SvgSearchWikidata.vue"
import WikidataSearchDialog from "./WikidataSearchDialog.vue"

export default defineComponent({
  props: {
    searchKey: { type: String, required: true },
    modelValue: String,
    errorMessage: String,
    currentTitle: { type: String, default: "" },
  },
  emits: ["selected", "update:modelValue"],
  components: {
    TextInput,
    SvgSearchWikidata,
    WikidataSearchDialog,
  },
  data() {
    return {
      showDialog: false,
    }
  },
  methods: {
    openDialog() {
      this.showDialog = true
    },
    closeDialog() {
      this.showDialog = false
    },
    handleSelected(
      entity: WikidataSearchEntity,
      titleAction?: "replace" | "append" | "neither"
    ) {
      this.showDialog = false
      this.$emit("selected", entity, titleAction)
    },
  },
})
</script>


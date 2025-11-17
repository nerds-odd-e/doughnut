<template>
  <button
    title="Wikidata Id"
    type="button"
    class="daisy-btn daisy-btn-outline daisy-btn-neutral daisy-rounded-l-none"
    @click.prevent="openDialog"
  >
    <SvgWikidata />
  </button>
  <WikidataSearchDialog
    v-if="showDialog"
    :search-key="searchKey"
    :current-title="currentTitle"
    :model-value="modelValue"
    :error-message="errorMessage"
    @close="closeDialog"
    @selected="handleSelected"
    @update:model-value="handleUpdate"
  />
</template>

<script lang="ts">
import type { WikidataSearchEntity } from "@generated/backend"
import { defineComponent } from "vue"
import SvgWikidata from "../svgs/SvgWikidata.vue"
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
    SvgWikidata,
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
    handleUpdate(value: string) {
      this.$emit("update:modelValue", value)
    },
    handleSelected(
      entity: WikidataSearchEntity,
      titleAction?: "replace" | "append"
    ) {
      this.showDialog = false
      this.$emit("selected", entity, titleAction)
    },
  },
})
</script>


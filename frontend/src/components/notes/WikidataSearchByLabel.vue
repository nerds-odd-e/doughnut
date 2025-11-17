<template>
  <button
    title="Wikidata Id"
    type="button"
    class="daisy-btn daisy-btn-outline daisy-btn-neutral"
    @click.prevent="openDialog"
  >
    <SvgSearchWikidata />
  </button>
  <WikidataSearchDialog
    v-if="showDialog"
    :search-key="searchKey"
    :current-title="currentTitle"
    :current-wikidata-id="modelValue"
    :error-message="errorMessage"
    @close="closeDialog"
    @selected="handleSelected"
    @update:wikidata-id="handleWikidataIdUpdate"
  />
</template>

<script lang="ts">
import type { WikidataSearchEntity } from "@generated/backend"
import { defineComponent, nextTick } from "vue"
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
    async handleSelected(
      entity: WikidataSearchEntity,
      titleAction?: "replace" | "append" | "neither"
    ) {
      this.$emit("selected", entity, titleAction)
      await nextTick()
      this.showDialog = false
    },
    handleWikidataIdUpdate(value: string) {
      this.$emit("update:modelValue", value)
    },
  },
})
</script>


<template>
  <button
    type="button"
    title="Wikidata Id"
    aria-label="Wikidata Id"
    :class="buttonClasses"
    @click.prevent="openDialog"
  >
    <SvgWikidata />
  </button>
  <WikidataAssociationDialog
    v-if="showDialog"
    :search-key="searchKey"
    :model-value="modelValue"
    :error-message="errorMessage"
    @close="closeDialog"
    @selected="handleSelected"
    @update:model-value="handleUpdate"
  />
</template>

<script lang="ts">
import type { WikidataSearchEntity } from "@generated/doughnut-backend-api"
import { defineComponent } from "vue"
import SvgWikidata from "../svgs/SvgWikidata.vue"
import { primeSoftKeyboard } from "@/utils/focusTarget"
import WikidataAssociationDialog from "./WikidataAssociationDialog.vue"
import { FIELD_JOIN_APPEND_BUTTON_CLASS } from "@/utils/fieldJoinAppendButtonClass"

export default defineComponent({
  props: {
    searchKey: { type: String, required: true },
    modelValue: String,
    errorMessage: String,
  },
  emits: ["selected", "update:modelValue"],
  components: {
    SvgWikidata,
    WikidataAssociationDialog,
  },
  computed: {
    hasWikidataId(): boolean {
      return !!this.modelValue && this.modelValue.trim() !== ""
    },
    buttonClasses(): string {
      if (this.errorMessage) {
        return "daisy-btn daisy-join-item daisy-btn-error"
      }
      if (this.hasWikidataId) {
        return "daisy-btn daisy-join-item daisy-btn-primary"
      }
      return FIELD_JOIN_APPEND_BUTTON_CLASS
    },
  },
  data() {
    return {
      showDialog: false,
    }
  },
  methods: {
    openDialog() {
      primeSoftKeyboard()
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


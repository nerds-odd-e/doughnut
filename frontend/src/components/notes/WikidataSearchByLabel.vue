<template>
  <TextInput
    scope-name="wikidataID"
    field="wikidataID"
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :errors="errors"
    placeholder="example: `Q1234`"
  >
    <template #input_prepend>
      <select
        v-if="wikiSearchSuggestions?.length > 0"
        ref="select"
        size="10"
        name="wikidataSearchResult"
        @change="onSelectSearchResult"
        @blur="removeSearchSuggestions"
        v-model="selectedOption"
        class="popup-select"
      >
        <option disabled value="">- Choose Wikidata Search Result -</option>
        <option
          v-for="suggestion in wikiSearchSuggestions"
          :key="suggestion.id"
          :value="suggestion.id"
          scope-name="searchItem"
        >
          {{ suggestion.label }} - {{ suggestion.description }}
        </option>
      </select>
      <button
        title="Wikidata Id"
        type="button"
        class="btn btn-outline-secondary"
        @click.prevent="fetchSearchResult"
      >
        <SvgSearchWikidata />
      </button>
    </template>
  </TextInput>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import TextInput from "../form/TextInput.vue";
import SvgSearchWikidata from "../svgs/SvgSearchWikidata.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    title: { type: String, required: true },
    modelValue: String,
    errors: String,
  },
  emits: ["selected", "update:modelValue"],
  components: {
    TextInput,
    SvgSearchWikidata,
  },
  data() {
    return {
      wikiSearchSuggestions: [] as Generated.WikidataSearchEntity[],
      selectedOption: "",
    };
  },
  methods: {
    async onSelectSearchResult() {
      const selectedSuggestion = this.wikiSearchSuggestions.find((obj) => {
        return obj.id === this.selectedOption;
      });
      this.wikiSearchSuggestions = [];
      if (!selectedSuggestion) return;
      this.$emit("selected", selectedSuggestion);
      this.selectedOption = "";
    },
    removeSearchSuggestions() {
      this.wikiSearchSuggestions = [];
    },
    async fetchSearchResult() {
      this.wikiSearchSuggestions = await this.api.wikidata.getWikidatas(
        this.title,
      );
      this.$nextTick(() => {
        const select = this.$refs.select as HTMLSelectElement | undefined;
        select?.focus();
      });
    },
  },
});
</script>

<style lang="scss" scoped>
.popup-select {
  width: 100%;
  position: absolute;
  top: 0;
  left: 0;
  z-index: 9999;
  cursor: pointer;
}
</style>

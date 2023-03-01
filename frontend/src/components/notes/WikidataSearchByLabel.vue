<template>
  <TextInput
    scope-name="wikidataID"
    field="wikidataID"
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :errors="errors"
    placeholder="example: `Q1234`"
  >
    <template #input_prepend
      ><button
        title="Wikidata Id"
        class="btn btn-outline-secondary"
        @click.prevent="fetchSearchResult"
      >
        <SvgSearchWikidata />
      </button>
    </template>
  </TextInput>
  <div class="row mt-2 mb-2">
    <div class="col-6">
      <select
        v-if="wikiSearchSuggestions?.length > 0"
        name="wikidataSearchResult"
        @change="onSelectSearchResult"
        class="form-control"
        v-model="selectedOption"
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
    </div>
  </div>
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
      if (!selectedSuggestion) return;
      this.$emit("selected", selectedSuggestion);
      this.selectedOption = "";
    },
    async fetchSearchResult() {
      this.wikiSearchSuggestions = await this.api.wikidata.getWikidatas(
        this.title
      );
    },
  },
});
</script>

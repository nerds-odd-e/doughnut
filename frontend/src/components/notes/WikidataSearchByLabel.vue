<template>
  <div class="row mt-2 mb-2">
    <div class="col-6 btn-group" role="group" aria-label="Action Group">
      <input type="submit" value="Submit" class="btn btn-primary" />
      <button
        id="search-wikidata"
        class="btn"
        @click.prevent="fetchSearchResult"
      >
        Search on Wikidata
      </button>
    </div>
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
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: { title: { type: String, required: true } },
  emits: ["selected"],
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

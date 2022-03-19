<template>
  <div>
    <TextInput
      scopeName="searchTerm"
      field="searchKey"
      v-model="searchTerm.searchKey"
      placeholder="Search"
    />
    <CheckInput scopeName="searchTerm" field="searchGlobally" v-model="searchTerm.searchGlobally" />
  </div>

  <div v-if="!searchResult || searchResult.length === 0">
    <em>No linkable notes found.</em>
  </div>
  <Cards v-else class="search-result" :notes="searchResult" columns="3">
    <template #button="{ note }">
      <button class="btn btn-primary" v-on:click="$emit('selected', note)">Select</button>
    </template>
  </Cards>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import TextInput from "../form/TextInput.vue";
import CheckInput from "../form/CheckInput.vue";
import Cards from "../notes/Cards.vue";
import _ from "lodash";
import useLoadingApi from '../../managedApi/useLoadingApi';


const debounced = _.debounce((callback) => callback(), 500);

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  name: "SearchNote",
  props: { noteId: Number },
  components: { TextInput, CheckInput, Cards },
  emits: ["selected"],
  data() {
    return {
      searchTerm: {
        searchKey: "",
        searchGlobally: false,
      } as Generated.SearchTerm,
      cache: {
        global: {},
        local: {}

      } as {
        global: Record<string, Generated.Note[]>,
        local: Record<string, Generated.Note[]>,
      },
      recentResult: undefined as Generated.Note[] | undefined,
    };
  },
  watch: {
    searchTerm: {
      handler(newSearchTerm) {
        if (newSearchTerm.searchKey.trim() === "") {
        } else {
          this.search();
        }
      },
      deep: true,
    },
  },
  computed: {
    trimmedSearchKey() {
      return this.searchTerm.searchKey.trim()
    },
    cachedSearches() {
      return this.searchTerm.searchGlobally ? this.cache.global : this.cache.local
    },
    cachedResult() {
      return this.cachedSearches[
        this.trimmedSearchKey
      ];
    },
    searchResult() {
      return !!this.cachedResult ? this.cachedResult : this.recentResult;
    },
  },
  methods: {
    search() {
      if (this.cachedSearches.hasOwnProperty(this.trimmedSearchKey)) {
        return;
      }

      debounced(async () => {
        const result = await this.api.relativeSearch({...this.searchTerm, note: this.noteId})
        this.recentResult = result
        this.cachedSearches[this.trimmedSearchKey] = result
      });
    },
  },
});
</script>

<style scoped>
.search-result {
  max-height: 300px;
  overflow-y: auto;
}
</style>

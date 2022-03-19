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
      recentResult: null,
    };
  },
  watch: {
    searchTerm: {
      handler(newSearchTerm) {
        if (newSearchTerm.searchKey.trim() === "") {
        } else {
          this.search(
            newSearchTerm.searchGlobally,
            newSearchTerm.searchKey.trim()
          );
        }
      },
      deep: true,
    },
  },
  computed: {
    cachedSearches() {
      return this.searchTerm.searchGlobally ? this.cache.global : this.cache.local
    },
    cachedResult() {
      return this.cachedSearches[
        this.searchTerm.searchKey.trim()
      ];
    },
    searchResult() {
      return !!this.cachedResult ? this.cachedResult : this.recentResult;
    },
  },
  methods: {
    search(searchGlobally, trimedSearchKey) {
      if (this.cachedSearches.hasOwnProperty(trimedSearchKey)) {
        return;
      }

      debounced(() => {
        this.api.relativeSearch(this.noteId,
          { searchGlobally, searchKey: trimedSearchKey },
        ).then((r) => {
          this.cachedSearches[trimedSearchKey] = r;
          this.recentResult = r;
        });
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

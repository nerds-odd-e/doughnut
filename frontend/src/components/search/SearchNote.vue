<template>
  <div>
    <TextInput
      scopeName="searchTerm"
      field="searchKey"
      v-model="searchTerm.searchKey"
      placeholder="Search"
    />
    <CheckInput
      scopeName="searchTerm"
      field="searchGlobally"
      v-model="searchTerm.searchGlobally"
    />
  </div>

  <div v-if="!searchResult || searchResult.length === 0">
    <em>No linkable notes found.</em>
  </div>
  <Cards v-else class="search-result" :notes="searchResult" columns="3">
    <template #button="{ note }">
      <button class="btn btn-primary" v-on:click="$emit('selected', note)">
        Select
      </button>
    </template>
  </Cards>
</template>

<script>
import TextInput from "../form/TextInput.vue";
import CheckInput from "../form/CheckInput.vue";
import Cards from "../notes/Cards.vue";
import _ from "lodash";
import useLoadingApi from '../../managedApi/useLoadingApi';

const debounced = _.debounce((callback) => callback(), 500);

export default {
  setup() {
    return useLoadingApi();
  },
  name: "SearchNote",
  props: { noteId: [String, Number] },
  components: { TextInput, CheckInput, Cards },
  emits: ["selected"],
  data() {
    return {
      searchTerm: {
        searchKey: "",
        searchGlobally: false,
      },
      cache: { true: {}, false: {} },
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
    cachedResult() {
      return this.cache[this.searchTerm.searchGlobally][
        this.searchTerm.searchKey.trim()
      ];
    },
    searchResult() {
      return !!this.cachedResult ? this.cachedResult : this.recentResult;
    },
  },
  methods: {
    search(searchGlobally, trimedSearchKey) {
      if (this.cache[searchGlobally].hasOwnProperty(trimedSearchKey)) {
        return;
      }

      debounced(() => {
        this.apiExp().relativeSearch(this.noteId,
          { searchGlobally, searchKey: trimedSearchKey },
        ).then((r) => {
          this.cache[searchGlobally][trimedSearchKey] = r;
          this.recentResult = r;
        });
      });
    },
  },
};
</script>

<style scoped>
.search-result {
  max-height: 300px;
  overflow-y: auto;
}
</style>

<template>
  <div>
    <CheckInput scopeName="searchTerm" field="allMyNotebooksAndSubscriptions" v-model="searchTerm.allMyNotebooksAndSubscriptions"
      :disabled="!noteId"
     />
    <CheckInput scopeName="searchTerm" field="allMyCircles" v-model="searchTerm.allMyCircles" />
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
import CheckInput from "../form/CheckInput.vue";
import Cards from "../notes/Cards.vue";
import _, { DebouncedFunc } from "lodash";
import useLoadingApi from '../../managedApi/useLoadingApi';


const debounced = _.debounce((callback) => callback(), 500);
let debouncedSearch: DebouncedFunc<any> | undefined

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  name: "SearchNote",
  props: { noteId: Number, inputSearchKey: {type: String, required: true } },
  components: { CheckInput, Cards },
  emits: ["selected"],
  data() {
    return {
      searchTerm: {
        searchKey: "",
        allMyNotebooksAndSubscriptions: false,
        allMyCircles: false,
      } as Generated.SearchTerm,
      oldSearchTerm: {
        searchKey: "",
        allMyNotebooksAndSubscriptions: false,
        allMyCircles: false,
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
      handler() {
        if (this.searchTerm.allMyCircles && !this.oldSearchTerm.allMyNotebooksAndSubscriptions) {
          this.searchTerm.allMyNotebooksAndSubscriptions = true
        }
        else if (!this.searchTerm.allMyNotebooksAndSubscriptions && this.oldSearchTerm.allMyCircles) {
          this.searchTerm.allMyCircles = false
        }
        if (this.searchTerm.searchKey.trim() === "") {
        } else {
          this.search();
        }
        this.oldSearchTerm = { ... this.searchTerm}
      },
      deep: true,
    },
    inputSearchKey() {
      this.searchTerm.searchKey = this.inputSearchKey
    }
  },
  computed: {
    trimmedSearchKey() {
      return this.searchTerm.searchKey.trim()
    },
    cachedSearches() {
      return this.searchTerm.allMyNotebooksAndSubscriptions ? this.cache.global : this.cache.local
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

      if(debouncedSearch) debouncedSearch.cancel();
      debouncedSearch = debounced(async () => {
        const originalTrimmedKey = this.trimmedSearchKey
        const result = await this.api.relativeSearch({...this.searchTerm, note: this.noteId})
        this.recentResult = result
        this.cachedSearches[originalTrimmedKey] = result
      });
    },
  },
  mounted() {
    if (!this.noteId) {
      this.searchTerm.allMyNotebooksAndSubscriptions = true
    }
    this.searchTerm.searchKey = this.inputSearchKey
  },
  beforeUnmount() {
    if(debouncedSearch) debouncedSearch.cancel();
  }
});
</script>

<style scoped>
.search-result {
  max-height: 300px;
  overflow-y: auto;
}
</style>

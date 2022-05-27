<template>
  <div>
      <TextInput
        scopeName="wikidataTitle"
        field="wikidataTitle"
        v-model="wikidataInput"
        placeholder="Search..."
      />
      <button class="btn btn-secondary" @click="searchWikidata()">Search</button>
      <select v-if="wikidataItems.length" multiple @change="onChange($event)">
          <option v-for="wikidataItem in wikidataItems" :key="wikidataItem.id" :value="wikidataItem.id">{{ wikidataItem.label }}</option>
      </select>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import TextInput from "../form/TextInput.vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";


export default defineComponent({
  setup() {
    return useStoredLoadingApi({ initalLoading: true, hasFormError: true });
  },
  props: { note: {type: Object as PropType<Generated.Note>, required: true } },
  components: { TextInput },
  emits: ["selected"],
  data() {
    return {
        wikidataInput: "",
        wikidataItems: [] as Generated.WikiDataSearchResponseModel[],
        wikiID: ""
    };
  },
  methods: {
    searchWikidata() {
      this.storedApi
      .searchWikidata(this.wikidataInput)
      .then((res) => {
        this.wikidataItems = res;
      })
    },
    onChange(event) {
      this.$emit("selected", event.target.value);
    }
  }
});
</script>
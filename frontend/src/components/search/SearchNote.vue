<template>
  <div>
    <TextInput
      scopeName="searchTerm"
      field="searchKey"
      v-model="inputSearchKey"
      placeholder="Search"
    />
    <SearchResults v-bind="{noteId, inputSearchKey}" @selected="$emit('selected', $event)"/>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import TextInput from "../form/TextInput.vue";
import SearchResults from "./SearchResults.vue";

export default defineComponent({
  props: { noteId: Number },
  components: { TextInput, SearchResults },
  emits: ["selected"],
  data() {
    return {
      inputSearchKey: "",
    };
  },
});
</script>
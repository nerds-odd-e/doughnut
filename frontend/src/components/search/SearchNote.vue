<template>
  <div>
    <TextInput
      scope-name="searchTerm"
      field="searchKey"
      v-model="inputSearchKey"
      placeholder="Search"
      v-focus
    />
    <SearchResults v-bind="{ noteId, inputSearchKey }">
      <template #button="{ noteTopic }">
        <button
          class="btn btn-primary"
          @click.prevent="$emit('selected', noteTopic)"
        >
          Select
        </button>
      </template>
    </SearchResults>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue"
import TextInput from "../form/TextInput.vue"
import SearchResults from "./SearchResults.vue"

export default defineComponent({
  props: { noteId: Number },
  components: { TextInput, SearchResults },
  emits: ["selected"],
  data() {
    return {
      inputSearchKey: "",
    }
  },
})
</script>

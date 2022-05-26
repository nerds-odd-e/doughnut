<template>
  <h3>
    Associate <strong>{{ note.title }}</strong> to WIKI
  </h3>
  <form v-if="!showConfirmation" @submit.prevent.once="validateAssociation">
    <TextInput scopeName="wikiID" field="wikiID" v-model="associationData.wikidataId" placeholder="Q1234" />
    <input type="submit" value="Save" class="btn btn-primary" />
  </form>

  <form v-else @submit.prevent.once="saveWiki">
    <p>Confirm to associate <strong>{{ note.title }}</strong> with <strong>{{ wikiDataTitle }}</strong>?</p>

    <input type="cancel" value="Cancel" class="btn btn-secondary" @click="showConfirmation = false"/>
    <input type="submit" value="Confirm" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import TextInput from "../form/TextInput.vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import SearchWikidata from "../search/SearchWikidata.vue";

export default defineComponent({
  setup() {
    return useStoredLoadingApi({ initalLoading: true, hasFormError: true });
  },
  props: { note: { type: Object as PropType<Generated.Note>, required: true } },
  components: { TextInput, SearchWikidata },
  emits: ["done"],
  data() {
    return {
      noteId: 0,
      associationData: {
        wikidataId: ""
      } as Generated.WikidataAssociationCreation,
      wikiDataTitle: "",
      showConfirmation: false,
    };
  },
  computed: {
    payload() {
      return {
        noteId: this.note.id,
        associationData: {
          wikidataId: this.associationData.wikidataId,
        }
      };
    },
  },
  methods: {
    validateAssociation() {
      this.storedApi.getWikiData(this.payload.associationData.wikidataId)
        .then((res: Generated.WikiDataDto) => {
          if (res.WikiDataTitleInEnglish !== this.note.title) {
            this.wikiDataTitle = res.WikiDataTitleInEnglish
            this.showConfirmation = true
          } else {
            this.saveWiki()
          }
        })
    },
    saveWiki() {
      this.storedApi
        .updateWikidataId(this.payload.noteId, this.payload.associationData)
        .then(() => {
          this.$emit("done");
        })
    }
  },
});
</script>

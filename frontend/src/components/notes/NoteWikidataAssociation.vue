<template>
  <a
    class="btn"
    role="button"
    title="Wiki Association"
    @click="onClickWikidata"
  >
    <SvgAssociation />
  </a>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import SvgAssociation from "../svgs/SvgAssociation.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import nonBlockingPopup from "../../managedApi/window/nonBlockingPopup";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    wikidataId: { type: String, required: true },
  },
  components: {
    SvgAssociation,
  },
  methods: {
    onClickWikidata() {
      nonBlockingPopup(this.wikiUrl());
    },
    async wikiUrl() {
      const wikipediaEnglishUrl = await this.getWikidataItem();
      if (wikipediaEnglishUrl !== "") {
        return wikipediaEnglishUrl;
      }
      return `https://www.wikidata.org/wiki/${this.wikidataId}`;
    },
    async getWikidataItem() {
      return (await this.api.wikidata.getWikidataEntityById(this.wikidataId))
        .WikipediaEnglishUrl;
    },
  },
});
</script>

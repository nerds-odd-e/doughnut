<template>
  <a class="btn" role="button" title="Wikidata" @click="onClickWikidata">
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
      const wikipediaEnglishUrl = await this.getWikiDataItem();
      if (wikipediaEnglishUrl !== "") {
        return wikipediaEnglishUrl;
      }
      return `https://www.wikidata.org/wiki/${this.wikidataId}`;
    },
    async getWikiDataItem() {
      return (await this.api.wikidata.getWikiData(this.wikidataId))
        .WikipediaEnglishUrl;
    },
  },
});
</script>

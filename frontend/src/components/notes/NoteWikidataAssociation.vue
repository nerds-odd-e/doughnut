<template>
  <button
    class="btn btn-sm"
    role="button"
    title="Wiki Association"
    @click="onClickWikidata"
  >
    <SvgAssociation />
    Go to Wikidata
  </button>
</template>

<script lang="ts">
import SvgAssociation from "@/components/svgs/SvgAssociation.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import nonBlockingPopup from "@/managedApi/window/nonBlockingPopup"
import { defineComponent } from "vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
  props: {
    wikidataId: { type: String, required: true },
  },
  components: {
    SvgAssociation,
  },
  methods: {
    onClickWikidata() {
      nonBlockingPopup(this.wikiUrl())
    },
    async wikiUrl() {
      const wikipediaEnglishUrl = await this.getWikidataItem()
      if (wikipediaEnglishUrl !== "") {
        return wikipediaEnglishUrl
      }
      return `https://www.wikidata.org/wiki/${this.wikidataId}`
    },
    async getWikidataItem() {
      return (
        await this.managedApi.restWikidataController.fetchWikidataEntityDataById(
          this.wikidataId
        )
      ).WikipediaEnglishUrl
    },
  },
})
</script>

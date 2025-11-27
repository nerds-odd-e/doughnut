<template>
  <button
    class="daisy-btn daisy-btn-sm daisy-btn-ghost daisy-gap-2"
    role="button"
    title="Wiki Association"
    @click="onClickWikidata"
  >
    <SvgAssociation />
    Go to Wikidata
  </button>
</template>

<script setup lang="ts">
import SvgAssociation from "@/components/svgs/SvgAssociation.vue"
import { WikidataController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import nonBlockingPopup from "@/managedApi/window/nonBlockingPopup"

const props = defineProps<{
  wikidataId: string
}>()

const getWikidataItem = async () => {
  const { data: entityData, error } =
    await WikidataController.fetchWikidataEntityDataById({
      path: { wikidataId: props.wikidataId },
    })
  if (!error && entityData) {
    return entityData.WikipediaEnglishUrl
  }
  return ""
}

const wikiUrl = async () => {
  const wikipediaEnglishUrl = await getWikidataItem()
  if (wikipediaEnglishUrl !== "") {
    return wikipediaEnglishUrl
  }
  return `https://www.wikidata.org/wiki/${props.wikidataId}`
}

const onClickWikidata = () => {
  nonBlockingPopup(wikiUrl())
}
</script>

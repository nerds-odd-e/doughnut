<template>
  <button
    class="daisy-btn daisy-btn-sm daisy-btn-ghost daisy-gap-2"
    role="button"
    title="Wiki Association"
    @click="onClickWikidata"
  >
    <Link2 class="w-5 h-5" />
    Go to Wikidata
  </button>
</template>

<script setup lang="ts">
import { Link2 } from "lucide-vue-next"
import { WikidataController } from "@generated/doughnut-backend-api/sdk.gen"
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

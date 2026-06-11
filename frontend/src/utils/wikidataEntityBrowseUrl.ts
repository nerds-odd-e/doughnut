import { WikidataController } from "@generated/doughnut-backend-api/sdk.gen"
import {} from "@/managedApi/clientSetup"
import nonBlockingPopup from "@/managedApi/window/nonBlockingPopup"

export async function resolveWikidataEntityBrowseUrl(
  wikidataId: string
): Promise<string> {
  const id = wikidataId.trim()
  const { data: entityData, error } =
    await WikidataController.fetchWikidataEntityDataById({
      path: { wikidataId: id },
    })
  if (!error && entityData) {
    const wikipediaEnglishUrl = entityData.WikipediaEnglishUrl?.trim()
    if (wikipediaEnglishUrl) {
      return wikipediaEnglishUrl
    }
  }
  return `https://www.wikidata.org/wiki/${id}`
}

export function openWikidataEntityBrowseUrlInNonBlockingPopup(
  wikidataId: string
): Promise<string> {
  const id = wikidataId.trim()
  if (!id) return Promise.resolve("")
  const p = resolveWikidataEntityBrowseUrl(id)
  nonBlockingPopup(p)
  return p
}

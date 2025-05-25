import type { WikidataEntityData } from "generated/backend"
import Builder from "./Builder"

class WikidataEntityBuilder extends Builder<WikidataEntityData> {
  data: WikidataEntityData

  constructor() {
    super()
    this.data = {
      WikidataTitleInEnglish: "default title",
      WikipediaEnglishUrl: "",
    }
  }

  wikidataTitle(value: string) {
    this.data.WikidataTitleInEnglish = value
    return this
  }

  do() {
    return this.data
  }
}

export default WikidataEntityBuilder

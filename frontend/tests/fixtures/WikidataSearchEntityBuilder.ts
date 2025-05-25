import type { WikidataSearchEntity } from "generated/backend"
import Builder from "./Builder"

class WikidataSearchEntityBuilder extends Builder<WikidataSearchEntity> {
  data: WikidataSearchEntity

  constructor() {
    super()
    this.data = {
      id: "Q1234",
      label: "label",
      description: "details",
    }
  }

  id(value: string) {
    this.data.id = value
    return this
  }

  label(value: string) {
    this.data.label = value
    return this
  }

  do() {
    return this.data
  }
}

export default WikidataSearchEntityBuilder

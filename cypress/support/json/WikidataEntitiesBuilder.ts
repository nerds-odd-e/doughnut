class WikidataEntitiesBuilder {
  claims: Record<string, unknown> = {}
  wikidataId: string

  constructor(wikidataId: string) {
    this.wikidataId = wikidataId
  }

  wclaims(claims: Record<string, unknown>): WikidataEntitiesBuilder {
    this.claims = claims
    return this
  }

  build(): unknown {
    return {
      entities: {
        [this.wikidataId]: {
          type: "item",
          id: this.wikidataId,
          claims: this.claims,
        },
      },
    }
  }
}

export default WikidataEntitiesBuilder

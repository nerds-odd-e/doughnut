interface Claim {
  claimId: string
  type: string
  value: unknown
}

class WikidataEntitiesBuilder {
  claims: Record<string, Array<unknown>> = {}
  wikidataId: string

  constructor(wikidataId: string) {
    this.wikidataId = wikidataId
  }

  wclaims(claims: Claim[]): WikidataEntitiesBuilder {
    this.claims = claims.reduce<Record<string, Array<unknown>>>((claimsIter, claimIter) => {
      const toClaim = {
        [claimIter.claimId]: [
          ...(claimsIter[claimIter.claimId] || []),
          {
            mainsnak: {
              snaktype: "value",
              property: claimIter.claimId,
              datavalue: {
                value: claimIter.value,
                type: claimIter.type,
              },
            },
          },
        ],
      }
      return { ...claimsIter, ...toClaim }
    }, this.claims)
    console.log(this.claims)
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
export { Claim }

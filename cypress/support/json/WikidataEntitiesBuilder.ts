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
      return { ...claimsIter, ...this.toClaim(claimIter, claimsIter[claimIter.claimId]) }
    }, this.claims)
    return this
  }

  private toClaim(claimIter: Claim, data: Array<unknown>): Record<string, Array<unknown>> {
    return {
      [claimIter.claimId]: [
        ...(data || []),
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

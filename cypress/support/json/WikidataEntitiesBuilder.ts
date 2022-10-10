interface Claim {
  claimId: string
  type: string
  value: unknown
}

const claim = (claimValue: Claim) => {
  return {
    [claimValue.claimId]: [
      {
        mainsnak: {
          snaktype: "value",
          property: claimValue.claimId,
          datavalue: {
            value: claimValue.value,
            type: claimValue.type,
          },
        },
      },
    ],
  }
}

class WikidataEntitiesBuilder {
  claims: Record<string, unknown> = {}
  wikidataId: string

  constructor(wikidataId: string) {
    this.wikidataId = wikidataId
  }

  wclaims(claims: Claim[]): WikidataEntitiesBuilder {
    this.claims = claims.reduce<Record<string, unknown>>((claimsIter, claimIter) => {
      return { ...claimsIter, ...claim(claimIter) }
    }, this.claims)
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
export { Claim, claim }

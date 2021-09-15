import Builder from "./Builder"

class LinkBuilder extends Builder{
    linkType: string

    cnt: number

    isReverse: boolean

    constructor(parentBuilder: Builder, linkType: string) {
      super(parentBuilder)
      this.linkType = linkType
      this.cnt = 1
      this.isReverse = false
    }

    count(cnt: number): LinkBuilder {
        this.cnt = cnt
        return this;
    }

    get reverse(): LinkBuilder {
      this.isReverse = true
      return this
    }

    do(): any {
        return {
            [this.linkType]: {
              [this.isReverse ? 'reverse' : 'direct']: Array.from({ length: this.cnt }, (x, i) => ({
                  id: 1938,
                  targetNote: {
                    id: 2423,
                    title: "a tool",
                  },
                  typeId: 15,
                  linkTypeLabel: "using",
                  linkNameOfSource: "user",
                })),
              [this.isReverse ? 'direct' : 'reverse']: [],
            },
          }
    }
}

export default LinkBuilder
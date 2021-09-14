import Builder from "./Builder"

class LinkBuilder extends Builder{
    linkType: string

    cnt: number

    constructor(parentBuilder: Builder, linkType: string) {
      super(parentBuilder)
      this.parentBuilder = parentBuilder
      this.linkType = linkType
      this.cnt = 1
    }

    count(cnt: number): LinkBuilder {
        this.cnt = cnt
        return this;
    }

    do(): any {
        return {
            [this.linkType]: {
              direct: Array.from({ length: this.cnt }, (x, i) => ({
                  id: 1938,
                  targetNote: {
                    id: 2423,
                    title: "a tool",
                  },
                  typeId: 15,
                  linkTypeLabel: "using",
                  linkNameOfSource: "user",
                })),
              reverse: [],
            },
          }
    }
}

export default LinkBuilder
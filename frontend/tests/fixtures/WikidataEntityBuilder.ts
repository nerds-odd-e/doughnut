import Builder from "./Builder";

class WikidataEntityBuilder extends Builder<Generated.WikidataEntity> {
  data: Generated.WikidataEntity;

  constructor() {
    super();
    this.data = {
      WikiDataTitleInEnglish: "default title",
      WikipediaEnglishUrl: "",
    };
  }

  wikidataTitle(value: string) {
    this.data.WikiDataTitleInEnglish = value;
    return this;
  }

  do() {
    return this.data;
  }
}

export default WikidataEntityBuilder;

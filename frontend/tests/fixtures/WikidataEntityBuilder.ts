import Builder from "./Builder";

class WikidataEntityBuilder extends Builder<Generated.WikidataEntity> {
  data: Generated.WikidataEntity;

  constructor() {
    super();
    this.data = {
      WikidataTitleInEnglish: "default title",
      WikipediaEnglishUrl: "",
    };
  }

  wikidataTitle(value: string) {
    this.data.WikidataTitleInEnglish = value;
    return this;
  }

  do() {
    return this.data;
  }
}

export default WikidataEntityBuilder;

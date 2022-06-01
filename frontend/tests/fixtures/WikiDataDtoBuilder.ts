import Builder from "./Builder";

class WikiDataDtoBuilder extends Builder<Generated.WikiDataDto> {
  data: Generated.WikiDataDto;

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

  do(): Generated.WikiDataDto {
    return this.data;
  }
}

export default WikiDataDtoBuilder;

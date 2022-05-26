import Builder from "./Builder";

class WikiDataDtoBuilder extends Builder<Generated.WikiDataDto> {
  data: Generated.WikiDataDto;

  constructor() {
    super();
    this.data = {
      WikiDataId: "Q12434",
      WikiDataTitleInEnglish: "Note1.1.1",
      WikipediaEnglishUrl: "",
    };
  }

  do(): Generated.WikiDataDto {
    return this.data;
  }
}

export default WikiDataDtoBuilder;

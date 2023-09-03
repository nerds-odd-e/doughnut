import Builder from "./Builder";

class WikidataSearchEntityBuilder extends Builder<Generated.WikidataSearchEntity> {
  data: Generated.WikidataSearchEntity;

  constructor() {
    super();
    this.data = {
      id: "Q1234",
      label: "label",
      description: "details",
    };
  }

  id(value: string) {
    this.data.id = value;
    return this;
  }

  label(value: string) {
    this.data.label = value;
    return this;
  }

  do() {
    return this.data;
  }
}

export default WikidataSearchEntityBuilder;

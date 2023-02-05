/// <reference types="cypress" />

import ServiceTester from "./ServiceTester"

class WikidataServiceTester extends ServiceTester {
  constructor() {
    super("wikidata")
  }
}

export default WikidataServiceTester

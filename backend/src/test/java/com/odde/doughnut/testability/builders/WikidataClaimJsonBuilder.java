package com.odde.doughnut.testability.builders;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WikidataClaimJsonBuilder {
  private String formatted = null;
  private String wikidataId;
  private String englishLabel = null;

  public WikidataClaimJsonBuilder country(String countryQId, String countryName) {
    this.wikidataId = countryQId;
    this.englishLabel =
        """
      "labels":{"en":{"language":"en","value":"%s"}}
      """.formatted(countryName);
    return this;
  }

  public String please() {
    return """
      {
        "entities": {
           "%s": {
              %s
           }
        }
      }
  """
        .formatted(
            wikidataId,
            Stream.of(englishLabel, formatted)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(",")));
  }

  public WikidataClaimJsonBuilder human(
      String humanId, String countryQId, String birthDayJsonObject) {
    this.wikidataId = humanId;
    this.formatted =
        """
              "claims": {
              "P27": [{
                 "mainsnak": {
                     "snaktype": "value",
                     "property": "P27",
                     "hash": "5e51bd61971a52beebe110cd5232eb4cb1a99a3f",
                     "datavalue": {
                         "value": {
                             "entity-type": "item",
                             "numeric-id": 865,
                             "id": "%s"
                         },
                         "type": "wikibase-entityid"
                     },
                     "datatype": "wikibase-item"
                 },
                 "type": "statement",
                 "id": "Q706446$B98C0820-A8FD-465F-93E0-3A6BF8A4A856",
                 "rank": "normal"
             }
              ],
                %s
                "P31": [
                  {
                    "mainsnak": {
                      "snaktype": "value",
                      "property": "P31",
                      "datavalue": {
                        "value": { "id": "Q5"},
                        "type": "wikibase-entityid"
                      }
                    }
                  }
                ]
              }
      """
            .formatted(countryQId, birthDayJsonObject);
    return this;
  }
}

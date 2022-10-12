package com.odde.doughnut.testability.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.platform.commons.util.StringUtils;

public class WikidataClaimJsonBuilder {
  private String wikidataId;
  private String englishLabel = null;
  private List<String> claims = new ArrayList<>();

  public WikidataClaimJsonBuilder(String wikidataId) {
    this.wikidataId = wikidataId;
  }

  public WikidataClaimJsonBuilder label(String name) {
    this.englishLabel =
        """
      "labels":{"en":{"language":"en","value":"%s"}}
      """.formatted(name);
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
            Stream.of(englishLabel, getClaims())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(",")));
  }

  private String getClaims() {
    if (claims.isEmpty()) {
      return null;
    }
    return "\"claims\":{" + String.join(",", claims) + "}";
  }

  private void addClaim(String property, String type, String value) {
    this.claims.add(
        """
                  "%s": [
                    {
                      "mainsnak": {
                        "snaktype": "value",
                        "property": "%s",
                        "datavalue": {
                          "value": %s,
                          "type": "%s"
                        }
                      }
                    }
                  ]
        """
            .formatted(property, property, value, type));
  }

  public WikidataClaimJsonBuilder countryOfOrigin(String countryQId) {

    this.addClaim(
        "P27",
        "wikibase-entityid",
        """
        {
          "entity-type": "item",
          "numeric-id": 865,
          "id": "%s"
        }
        """
            .formatted(countryQId));
    return this;
  }

  public WikidataClaimJsonBuilder asHuman() {
    this.addClaim("P31", "wikibase-entityid", "{\"id\": \"Q5\"}");
    return this;
  }

  public WikidataClaimJsonBuilder birthdayIf(String birthdayByISO) {
    if (!StringUtils.isBlank(birthdayByISO)) {
      this.addClaim("P569", "time", "{ \"time\": \"" + birthdayByISO + "\"}");
    }
    return this;
  }
}

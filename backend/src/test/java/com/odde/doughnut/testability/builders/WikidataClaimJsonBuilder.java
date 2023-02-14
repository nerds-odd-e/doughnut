package com.odde.doughnut.testability.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.util.Strings;
import org.junit.platform.commons.util.StringUtils;

public class WikidataClaimJsonBuilder {
  private String wikidataId;
  private String englishLabel = null;
  private List<String> claims = new ArrayList<>();

  public WikidataClaimJsonBuilder(String wikidataId) {
    this.wikidataId = wikidataId;
  }

  public WikidataClaimJsonBuilder labelIf(String name) {
    if (!Strings.isEmpty(name)) {
      this.englishLabel =
          """
          "labels":{"en":{"language":"en","value":"%s"}}
          """
              .formatted(name);
    }
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
    this.addClaimToTheSamePropertyOfTheSameType(property, type, List.of(value));
  }

  private void addClaimsOfWikiBaseEntityIds(String property, List<String> ids) {
    List<String> wikibaseIds =
        ids.stream()
            .map("""
                { "id": "%s" }
                """::formatted)
            .toList();
    this.addClaimToTheSamePropertyOfTheSameType(property, "wikibase-entityid", wikibaseIds);
  }

  private void addClaimToTheSamePropertyOfTheSameType(
      String property, String type, List<String> values) {
    String innerArray =
        values.stream()
            .map(
                value ->
                    """
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
          """
                        .formatted(property, value, type))
            .collect(Collectors.joining(","));
    this.claims.add(
        """
                "%s": [
                  %s
                ]
      """
            .formatted(property, innerArray));
  }

  public WikidataClaimJsonBuilder countryOfOrigin(String countryQId) {
    if (countryQId == null) return this;
    this.addClaimsOfWikiBaseEntityIds("P27", List.of(countryQId));
    return this;
  }

  public WikidataClaimJsonBuilder asAHuman() {
    this.addClaimsOfWikiBaseEntityIds("P31", List.of("Q5"));
    return this;
  }

  public WikidataClaimJsonBuilder birthdayIf(String birthdayByISO) {
    if (!StringUtils.isBlank(birthdayByISO)) {
      this.addClaim("P569", "time", "{ \"time\": \"" + birthdayByISO + "\"}");
    }
    return this;
  }

  public WikidataClaimJsonBuilder globeCoordinate(String value, String type) {
    this.addClaim("P625", type, value);
    return this;
  }

  public WikidataClaimJsonBuilder asABookWithSingleAuthor(String authorWikiDataId) {
    return asABookWithMultipleAuthors(List.of(authorWikiDataId));
  }

  public WikidataClaimJsonBuilder asABookWithMultipleAuthors(List<String> authorWikiDataIds) {
    this.addClaimsOfWikiBaseEntityIds("P50", authorWikiDataIds);
    return this;
  }
}

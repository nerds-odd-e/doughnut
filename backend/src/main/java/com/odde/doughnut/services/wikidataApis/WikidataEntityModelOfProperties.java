package com.odde.doughnut.services.wikidataApis;

import com.odde.doughnut.entities.Coordinate;
import com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataEntity;
import java.util.Optional;
import java.util.stream.Stream;

public class WikidataEntityModelOfProperties {
  private final WikidataEntity entity;

  public WikidataEntityModelOfProperties(WikidataEntity entity) {
    this.entity = entity;
  }

  public Optional<Coordinate> getGeographicCoordinate() {
    return entity.getFirstClaimValue("P625").map(WikidataValue::getCoordinate);
  }

  public Optional<WikidataId> getInstanceOf() {
    return entity.getFirstClaimValue("P31").map(WikidataValue::toWikiClass);
  }

  public Optional<Coordinate> getCoordinate() {
    return entity.getFirstClaimValue("P625").map(WikidataValue::getCoordinate);
  }

  public Optional<WikidataValue> getBirthdayValue() {
    return entity.getFirstClaimValue("P569");
  }

  public Optional<WikidataId> getCountryOfOriginValue() {
    return entity.getFirstClaimValue("P27").map(WikidataValue::toWikiClass);
  }

  public Stream<WikidataId> getAuthors() {
    return entity.getClaimValues("P50").map(WikidataValue::toWikiClass);
  }
}

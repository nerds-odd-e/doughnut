package com.odde.doughnut.services.wikidataApis;

import com.odde.doughnut.entities.Coordinate;
import com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataEntity;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WikidataEntityModel extends WikidataEntityModelOfProperties {
  public WikidataEntityModel(WikidataEntity entity) {
    super(entity);
  }

  private Boolean isHuman() {
    return getInstanceOf().map(WikidataId::isHuman).orElse(false);
  }

  private String getLocationDescription() {
    return getGeographicCoordinate().map(Coordinate::toLocationDescription).orElse(null);
  }

  private String getHumanDescription(WikidataApi wikidataApi) {
    return Stream.of(
            getCountryOfOriginValue()
                .flatMap(
                    wikidataId1 -> wikidataId1.withApi(wikidataApi).fetchEnglishTitleFromApi()),
            getBirthdayValue().map(WikidataValue::formattedTime))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(value -> !value.isBlank())
        .collect(Collectors.joining(", "));
  }

  public String wikidataDescription(WikidataApi wikidataApi1) {
    if (isHuman()) {
      return getHumanDescription(wikidataApi1);
    }
    return getLocationDescription();
  }
}

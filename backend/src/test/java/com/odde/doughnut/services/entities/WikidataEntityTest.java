package com.odde.doughnut.services.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.services.wikidataApis.WikidataValue;
import com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataClaimItem;
import com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataDatavalue;
import com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataEntity;
import com.odde.doughnut.services.wikidataApis.thirdPartyEntities.WikidataMainsnak;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WikidataEntityTest {
  WikidataDatavalue wikidataDataValue;
  WikidataEntity wikidataEntity;

  @BeforeEach
  void setup() {
    wikidataDataValue = new WikidataDatavalue();
    wikidataDataValue.setType("this is a value");

    WikidataMainsnak mainsnak = new WikidataMainsnak();
    mainsnak.setDatavalue(wikidataDataValue);

    WikidataClaimItem wikidataClaimItem = new WikidataClaimItem();
    wikidataClaimItem.setMainsnak(mainsnak);

    wikidataEntity = new WikidataEntity();
    wikidataEntity.setClaims(
        new HashMap<>() {
          {
            put("P31", Stream.of(wikidataClaimItem).collect(Collectors.toList()));
            put("P32", Collections.emptyList());
          }
        });
  }

  @Test
  void getFirstClaimValueWhenPropertyExists() {
    assertThat(
        wikidataEntity.getFirstClaimValue("P31"),
        equalTo(Optional.of(new WikidataValue(wikidataDataValue))));
  }

  @ParameterizedTest
  @ValueSource(strings = {"P32", "P33"})
  void getFirstClaimValueEmptyWhenMissingOrEmpty(String property) {
    assertThat(wikidataEntity.getFirstClaimValue(property), equalTo(Optional.empty()));
  }

  @Test
  void getClaimValuesWhenPropertyExists() {
    assertThat(
        wikidataEntity.getClaimValues("P31").toList(),
        equalTo(List.of(new WikidataValue(wikidataDataValue))));
  }

  @ParameterizedTest
  @ValueSource(strings = {"P32", "P33"})
  void getClaimValuesEmptyWhenMissingOrEmpty(String property) {
    assertThat(wikidataEntity.getClaimValues(property).toList(), empty());
  }
}

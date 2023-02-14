package com.odde.doughnut.services.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
  void getSingleValuePropertyIfThePropertyExists() {
    Optional<WikidataValue> result = wikidataEntity.getFirstClaimValue("P31");
    assertEquals(Optional.of(new WikidataValue(wikidataDataValue)), result);
  }

  @Test
  void getSingleValuePropertyIfThePropertyDoesNotExists() {
    Optional<WikidataValue> result = wikidataEntity.getFirstClaimValue("P33");
    assertEquals(Optional.empty(), result);
  }

  @Test
  void getSingleValuePropertyIfThePropertyIsEmpty() {
    Optional<WikidataValue> result = wikidataEntity.getFirstClaimValue("P32");
    assertEquals(Optional.empty(), result);
  }

  @Test
  void getMultipleValuePropertyIfThePropertyExists() {
    List<WikidataValue> result = wikidataEntity.getClaimValues("P31").toList();
    assertEquals(List.of(new WikidataValue(wikidataDataValue)), result);
  }

  @Test
  void getMultipleValuePropertyDoesNotExist() {
    List<WikidataValue> result = wikidataEntity.getClaimValues("P33").toList();
    assertEquals(Collections.emptyList(), result);
  }

  @Test
  void getMultipleValuePropertyIsEmpty() {
    List<WikidataValue> result = wikidataEntity.getClaimValues("P32").toList();
    assertEquals(Collections.emptyList(), result);
  }
}

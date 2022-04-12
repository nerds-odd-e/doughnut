package com.odde.doughnut.algorithms;

public class SiblingOrder {
  //
  // SiblingOrder is used to decide the order among the notes under the same
  // parent. In a real life situation, using millisecond * 1000 is good enough
  // for ordering and also leave enough space for inserting items in between.
  // However, in extreme cases like unit testing or importing batch of notes,
  // there is still a chance of duplicated order number. Since this will likely
  // to happy within the same java running instance, a simple static variable
  // `localLastOrderNumberForGoodEnoughSiblingOrder` is introduced to detect and
  // remove the duplicates.
  //
  public static long localLastOrderNumberForGoodEnoughSiblingOrder;
  public static final long MINIMUM_SIBLING_ORDER_INCREMENT = 1000;

  public static Long getGoodEnoughOrderNumber() {
    long newNumber = System.currentTimeMillis() * MINIMUM_SIBLING_ORDER_INCREMENT;
    if (newNumber <= localLastOrderNumberForGoodEnoughSiblingOrder) {
      localLastOrderNumberForGoodEnoughSiblingOrder += MINIMUM_SIBLING_ORDER_INCREMENT;
    } else {
      localLastOrderNumberForGoodEnoughSiblingOrder = newNumber;
    }
    return localLastOrderNumberForGoodEnoughSiblingOrder;
  }
}

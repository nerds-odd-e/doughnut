package com.odde.doughnut.entities;

import static org.hamcrest.Matchers.*;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NoteAsConstructionTest {
  // example full expression:
  //   "School of the Heart (Chinese: 心學), or Yangmingism,
  //   is one of the major philosophical schools of Neo-Confucianism,
  //   based on the ideas of the idealist Neo-Confucian philosopher
  //   Wang Shouren. Throughout the whole Yuan dynasty, as well as in
  //   the beginning of the Ming dynasty, the magistral philosophy in
  //   China was the Rationalistic School, another Neo-Confucianism school
  //   emphasizing the importance of observational science built by Cheng Yi
  //   and especially Zhu Xi. Wang Yangming, on the other hand, developed
  //   his philosophy as the main intellectual opposition to the Cheng-Zhu School.
  //   Yangmingism is considered to be part of the School of Mind established by
  //   Lu Jiuyuan, upon whom Yangming drew inspirations. Yangming argued that one
  //   can learn the supreme principle (理, pinyin: Li) from their minds,
  //   objecting to Cheng and Zhu's belief that one can only seek the supreme
  //   principle in the objective world. Furthermore, Yangmingism posits a
  //   oneness of action and knowledge in relation to one's concepts of morality.
  //   This idea, "regard the inner knowledge and the exterior action as one"
  //   (知行合一) is the main tenet in Yangmingism.

  // note %1: "School of the Heart (Chinese: 心學) / Yangmingism"
  // note %2: "Neo-Confucianism"
  // note %3: "philosophical schools"
  // note %4: "%3 of %2"
  // note %5: "the major %4"
  // note %6: "%1 is one of %5"
  // note %7: "Wang Shouren"
  // note %8: "idealist %2 philosopher"
  // note %9: "%7 is a %8"
  // note %9: "the ideas of %9"
  // note %10: "%1 is based on %9"

}

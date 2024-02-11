package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.User;

public record ParentGrandLinkHelperImpl(User user, LinkingNote link, LinkingNote parentGrandLink) {}

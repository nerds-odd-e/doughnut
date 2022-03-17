package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;

import javax.validation.Valid;

public class InitialInfo {
    @Valid
    public ReviewPoint reviewPoint;
    @Valid
    public ReviewSetting reviewSetting;
}

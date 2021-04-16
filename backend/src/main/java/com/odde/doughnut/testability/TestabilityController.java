
package com.odde.doughnut.testability;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;


@Controller
@Profile({"test", "dev"})
@RequestMapping("/testability")
class TestabilityController {
    @Autowired
    TimeTraveler timeTraveler;

    @PostMapping(value="/time_travel")
    public String timeTravel(@RequestParam Map<String, String> userInfo) {
        String travelTo = userInfo.get("travel_to");
        DateTimeFormatter formatter = TestabilityRestController.getDateTimeFormatter();
        LocalDateTime localDateTime = LocalDateTime.from(formatter.parse(travelTo));
        Timestamp timestamp = Timestamp.valueOf(localDateTime);
        timeTraveler.timeTravelTo(timestamp);
        return "redirect:panel";
    }

    @PostMapping(value="/randomizer")
    public String randomizer(@RequestParam Map<String, String> userInfo) {
        String option = userInfo.get("choose");
        timeTraveler.setAlwaysChoose(option);
        return "redirect:panel";
    }

    @GetMapping("/panel")
    public String panel(Model model) {
        String currentTime = timeTraveler.getCurrentUTCTimestamp().toLocalDateTime().format(TestabilityRestController.getDateTimeFormatter());
        model.addAttribute("currentTime", currentTime);

        return "testability/panel";
    }

    @GetMapping("/exception")
    public String exception(Model model) {
        throw new RuntimeException("for failure report");
    }

    // Cross Domain Test Screen
    @GetMapping("/issue")
    public String issue(Model model) {
        return "testability/issue";
    }
}

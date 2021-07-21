package com.odde.doughnut.algorithms;

class TitleFragment {
    private final String content;

    TitleFragment(String content) {
        String trimmed = content.trim();
        if(content.startsWith("~")) {
            this.content = trimmed.substring(1);
        }
        else {
            this.content = trimmed;
        }
    }

    String presence() {
        return content;
    }

    boolean matches(String answer) {
        return presence().equalsIgnoreCase(answer);
    }
}

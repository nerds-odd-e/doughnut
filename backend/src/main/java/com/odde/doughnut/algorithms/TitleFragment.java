package com.odde.doughnut.algorithms;

class TitleFragment {
    private final String content;

    TitleFragment(String content) {
        this.content = content.trim();
    }

    String presence() {
        return content;
    }

    boolean matches(String answer) {
        return presence().equalsIgnoreCase(answer);
    }
}

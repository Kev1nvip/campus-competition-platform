package com.competition.backend.common.constant;

public enum DocType {
    SIGNUP_GUIDE("报名须知"),
    PRELIMINARY("初赛说明"),
    FINAL("复赛说明"),
    SUPPLEMENTARY("补充材料");

    private final String label;

    DocType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

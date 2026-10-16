package com.example.studymate.ui;

public class ToolItem {
    public enum ActionType { LAUNCH_ACTIVITY, PICK_PPTX, PICK_PPTX_TO_PDF, PICK_DOCX, PICK_DOCX_TO_PDF }

    public final String title, subtitle;
    public final int iconRes, color;
    public final Class<?> target;
    public final ActionType actionType;

    public ToolItem(String title, String subtitle, int iconRes, int color, Class<?> target) {
        this(title, subtitle, iconRes, color, target, ActionType.LAUNCH_ACTIVITY);
    }

    public ToolItem(String title, String subtitle, int iconRes, int color, ActionType actionType) {
        this(title, subtitle, iconRes, color, null, actionType);
    }

    public ToolItem(String title, String subtitle, int iconRes, int color, Class<?> target, ActionType actionType) {
        this.title = title;
        this.subtitle = subtitle;
        this.iconRes = iconRes;
        this.color = color;
        this.target = target;
        this.actionType = actionType;
    }
}

package com.neostride.app.feature.search.model;

public class SearchItem {

    private String title;
    private String subText;
    private String type;
    private String category;

    public SearchItem(String title, String subText, String type, String category) {
        this.title = title;
        this.subText = subText;
        this.type = type;
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public String getSubText() {
        return subText;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }
}
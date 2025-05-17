package com.example.note_create_app;

public class Notes {
    private String id;
    private String title;
    private String content;
    private long date;

    public Notes() {}

    public Notes(String title, long date, String content) {
       this.title = title;
       this.date = date;
       this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String _getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

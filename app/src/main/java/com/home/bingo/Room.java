package com.home.bingo;

public class Room {
    String id;
    String title;
    int status;
    Member creator;
    Member joiner;

    public Room() {
    }

    public Room(String title, Member creator) {
        this.title = title;
        this.creator = creator;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Member getCreator() {
        return creator;
    }

    public void setCreator(Member creator) {
        this.creator = creator;
    }

    public Member getJoiner() {
        return joiner;
    }

    public void setJoiner(Member joiner) {
        this.joiner = joiner;
    }
}

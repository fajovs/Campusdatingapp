package com.ensias.essudatingapp;

import com.google.firebase.database.Exclude;

public class Match {

    @Exclude
    private String id;

    private String user1;
    private String user2;
    private Long timestamp;
    private String lastMessage;
    private Long lastMessageTimestamp;

    @Exclude
    private User otherUser;

    // Default constructor required for Firebase
    public Match() {
    }

    public Match(String user1, String user2, Long timestamp) {
        this.user1 = user1;
        this.user2 = user2;
        this.timestamp = timestamp;
    }

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    public String getUser1() {
        return user1;
    }

    public void setUser1(String user1) {
        this.user1 = user1;
    }

    public String getUser2() {
        return user2;
    }

    public void setUser2(String user2) {
        this.user2 = user2;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    @Exclude
    public User getOtherUser() {
        return otherUser;
    }

    @Exclude
    public void setOtherUser(User otherUser) {
        this.otherUser = otherUser;
    }
}
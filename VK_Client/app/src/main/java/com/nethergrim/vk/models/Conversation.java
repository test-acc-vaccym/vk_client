package com.nethergrim.vk.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * @author andreydrobyazko on 3/20/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Conversation extends RealmObject {

    private long unread;
    private Message message;

    @PrimaryKey
    /**
     * Id of the user, in case 1-1 conversation, Id of group chat in case of group chat
     * */
    private long id;
    private long date;

    public Conversation() {
    }

    public long getUnread() {
        return unread;
    }

    public void setUnread(long unread) {
        this.unread = unread;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
}
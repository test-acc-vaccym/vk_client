package com.nethergrim.vk.models;

import io.realm.RealmObject;

/**
 * @author andrej on 14.08.15.
 */
public class RealmString extends RealmObject {

    private String s;

    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }
}

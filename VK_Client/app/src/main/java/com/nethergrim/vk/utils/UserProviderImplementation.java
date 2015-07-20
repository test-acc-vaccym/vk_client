package com.nethergrim.vk.utils;

import com.nethergrim.vk.MyApplication;
import com.nethergrim.vk.caching.Prefs;
import com.nethergrim.vk.models.User;

import javax.inject.Inject;

import io.realm.Realm;

/**
 * @author Andrey Drobyazko (c2q9450@gmail.com).
 *         All rights reserved.
 */
public class UserProviderImplementation implements UserProvider {

    @Inject
    Prefs mPrefs;
    private Realm mRealm;

    public UserProviderImplementation() {
        MyApplication.getInstance().getMainComponent().inject(this);
        mRealm = Realm.getDefaultInstance();
    }

    @Override
    public User getUser(long userId) {
        return mRealm.where(User.class).equalTo("id", userId).findFirst();
    }

    @Override
    public User getMyUser() {
        return getUser(mPrefs.getCurrentUserId());
    }
}
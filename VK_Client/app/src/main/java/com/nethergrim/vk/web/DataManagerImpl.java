package com.nethergrim.vk.web;

import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.nethergrim.vk.Constants;
import com.nethergrim.vk.MyApplication;
import com.nethergrim.vk.caching.Prefs;
import com.nethergrim.vk.event.ConversationsUpdatedEvent;
import com.nethergrim.vk.event.FriendsUpdatedEvent;
import com.nethergrim.vk.event.MyUserUpdatedEvent;
import com.nethergrim.vk.event.UsersUpdatedEvent;
import com.nethergrim.vk.images.ImageLoader;
import com.nethergrim.vk.images.PaletteProvider;
import com.nethergrim.vk.models.ConversationsList;
import com.nethergrim.vk.models.ConversationsUserObject;
import com.nethergrim.vk.models.ListOfFriends;
import com.nethergrim.vk.models.ListOfUsers;
import com.nethergrim.vk.models.StartupResponse;
import com.nethergrim.vk.models.User;
import com.nethergrim.vk.utils.DataHelper;
import com.squareup.otto.Bus;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;
import rx.Observable;

/**
 * Class that will handle results of every web request. Should be used to map, persist data, and
 * notify UI thread to be updated.
 * <p>
 * This should be the only way to handle web requests results.
 * <p>
 * By default every method of current class should return {@link rx.Observable} as result.
 *
 * @author andrej on 30.08.15 (c2q9450@gmail.com).
 *         All rights reserved.
 */
public class DataManagerImpl implements DataManager {


    public static final String TAG = DataManager.class.getSimpleName();

    @Inject
    Prefs mPrefs;

    @Inject
    Bus mBus;

    @Inject
    ImageLoader mImageLoader;

    @Inject
    PaletteProvider mPaletteProvider;

    @Inject
    WebRequestManager mWebRequestManager;

    public DataManagerImpl() {
        MyApplication.getInstance().getMainComponent().inject(this);
    }

    @Override
    public Observable<StartupResponse> launchStartupTasksAndPersistToDb() {

        // prepare parameters
        String token = mPrefs.getGcmToken();
        if (TextUtils.isEmpty(token)) {
            InstanceID instanceID = InstanceID.getInstance(
                    MyApplication.getInstance().getApplicationContext());
            try {
                token = instanceID.getToken(Constants.GCM_SENDER_ID,
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mPrefs.setGcmToken(token);
        }

        // make web request
        StartupResponse startupResponse = mWebRequestManager.launchStartupTasks(token);
        if (startupResponse.ok()) {
            mPrefs.setCurrentUserId(startupResponse.getResponse().getMe().getId());
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(startupResponse.getResponse().getMe());
            realm.commitTransaction();
        } else {
            // TODO: 30.08.15 handle errors
            Log.e(TAG, "error: " + startupResponse.getError().toString());
        }

        mBus.post(new MyUserUpdatedEvent());
        return Observable.just(startupResponse);
    }

    @Override
    public Observable<ListOfFriends> fetchFriendsAndPersistToDb(int count, int offset) {
        ListOfFriends listOfFriends = mWebRequestManager.getFriends(
                mPrefs.getCurrentUserId(), count, offset);
        if (listOfFriends.ok()) {

            mPrefs.setFriendsCount(listOfFriends.getResponse().getCount());

            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            List<User> friends = listOfFriends.getResponse().getFriends();
            for (int i = 0, size = friends.size(), rating = offset;
                    i < size;
                    i++, rating++) {
                friends.get(i).setFriendRating(rating);
            }
            realm.copyToRealmOrUpdate(friends);
            realm.commitTransaction();
            mPaletteProvider.generateAndStorePalette(friends);
            for (int i = 0, size = friends.size(); i < size; i++) {
                mImageLoader.cacheUserAvatars(friends.get(i));
            }
            mBus.post(new FriendsUpdatedEvent(listOfFriends.getResponse().getCount()));
        } else {
            // TODO: 30.08.15 handle errors
            Log.e(TAG, "error: " + listOfFriends.getError().toString());
        }

        return Observable.just(listOfFriends);
    }

    @Override
    public Observable<ListOfUsers> fetchUsersAndPersistToDB(List<Long> ids) {
        ListOfUsers listOfUsers = mWebRequestManager.getUsers(ids);
        if (listOfUsers.ok()) {
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(listOfUsers.getResponse());
            realm.commitTransaction();
            mPaletteProvider.generateAndStorePalette(listOfUsers.getResponse());
            mBus.post(new UsersUpdatedEvent());
            for (int i = 0, size = listOfUsers.getResponse().size(); i < size; i++) {
                mImageLoader.cacheUserAvatars(listOfUsers.getResponse().get(i));
            }
        } else {
            // TODO: 30.08.15 handle errors
            Log.e(TAG, "error: " + listOfUsers.getError().toString());
        }
        return Observable.just(listOfUsers);
    }

    @Override
    public Observable<ConversationsUserObject> fetchConversationsUserAndPersist(int limit,
            int offset,
            boolean unreadOnly) {
        ConversationsUserObject conversationsUserObject
                = mWebRequestManager.getConversationsAndUsers(limit, offset, unreadOnly);
        if (conversationsUserObject.ok()) {

            //saving conversations to db
            ConversationsList conversationsList
                    = conversationsUserObject.getResponse().getConversations();
            conversationsList.setResults(
                    DataHelper.normalizeConversationsList(conversationsList.getResults()));
            mPrefs.setUnreadMessagesCount(conversationsList.getUnreadCount());
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(conversationsList.getResults());

            //saving users to db
            List<User> users = conversationsUserObject.getResponse().getUsers();
            realm.copyToRealmOrUpdate(users);

            realm.commitTransaction();
            mBus.post(new ConversationsUpdatedEvent());
            mBus.post(new UsersUpdatedEvent());
        } else {
            // TODO: 30.08.15 handle errors
            Log.e(TAG, conversationsUserObject.getError().toString());
        }
        return Observable.just(conversationsUserObject);
    }
}

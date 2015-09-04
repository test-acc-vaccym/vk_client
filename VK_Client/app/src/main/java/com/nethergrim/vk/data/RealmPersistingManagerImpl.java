package com.nethergrim.vk.data;

import android.support.annotation.NonNull;

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

import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;

/**
 * @author andrej on 30.08.15.
 */
public class RealmPersistingManagerImpl implements PersistingManager {

    @Inject
    Prefs mPrefs;

    @Inject
    Bus mBus;

    @Inject
    PaletteProvider mPaletteProvider;

    @Inject
    ImageLoader mImageLoader;

    public RealmPersistingManagerImpl() {
        MyApplication.getInstance().getMainComponent().inject(this);
    }

    @Override
    public void manage(@NonNull StartupResponse startupResponse) {
        mPrefs.setCurrentUserId(startupResponse.getResponse().getMe().getId());
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(startupResponse.getResponse().getMe());
        realm.commitTransaction();
        mBus.post(new MyUserUpdatedEvent());
    }

    @Override
    public void manage(@NonNull ListOfFriends listOfFriends, int offset) {
        mPrefs.setFriendsCount(listOfFriends.getResponse().getCount());

        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        List<User> friends = listOfFriends.getResponse().getFriends();
        for (int i = 0, size = friends.size(), rating = offset;
                i < size;
                i++, rating++) {
            friends.get(i).setFriendRating(rating);
            // TODO: 30.08.15 fix friends rating persistense, to make it consistent.
        }
        realm.copyToRealmOrUpdate(friends);
        realm.commitTransaction();
        mPaletteProvider.generateAndStorePalette(friends);
        for (int i = 0, size = friends.size(); i < size; i++) {
            mImageLoader.cacheUserAvatars(friends.get(i));
        }
        mBus.post(new FriendsUpdatedEvent(listOfFriends.getResponse().getCount()));
    }

    @Override
    public void manage(ListOfUsers listOfUsers) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(listOfUsers.getResponse());
        realm.commitTransaction();
        mPaletteProvider.generateAndStorePalette(listOfUsers.getResponse());
        mBus.post(new UsersUpdatedEvent());
        for (int i = 0, size = listOfUsers.getResponse().size(); i < size; i++) {
            mImageLoader.cacheUserAvatars(listOfUsers.getResponse().get(i));
        }
    }

    @Override
    public void manage(ConversationsUserObject conversationsUserObject) {
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
    }
}
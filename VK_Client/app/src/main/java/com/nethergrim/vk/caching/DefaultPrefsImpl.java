package com.nethergrim.vk.caching;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.nethergrim.vk.MyApplication;
import com.nethergrim.vk.enums.MainActivityState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * @author andreydrobyazko on 3/20/15.
 */
public class DefaultPrefsImpl implements Prefs {

    public static final String KEY_USER_ID = "id";
    public static final String KEY_ACTIVITY_STATE_ID = "activity_state_id";
    public static final String KEY_GCM_TOKEN = "gcm_token";
    public static final String KEY_UNREAD_MESSAGES_COUNT = "unread_messages_count";
    public static final String KEY_FRIENDS_COUNT = "fr_c";
    public static final String KEY_KEYBOARD_HEIGHT = "keyboard_height";
    public static final String KEY_FETCH_STICKERS_TIMESTAMP = "fetch_stickers_timestamp";
    public static final String KEY_EMOJI_TAB = "emoji_tab";
    public static final String KEY_MARK_MESSAGES_AS_READ = "mark_as_read";
    public static final String KEY_DISPLAY_UNREAD_AS_UNREAD = "display_unread_as_unread";
    public static final String KEY_SYNC_MARK_MESSAGES_TO_READ = "sync_mark_read";
    private static final String KEY_TOKEN = "token";
    private SharedPreferences mPrefs;

    public DefaultPrefsImpl() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(
                MyApplication.getInstance().getApplicationContext());
    }

    @Override
    public long getCurrentUserId() {
        return mPrefs.getLong(KEY_USER_ID, 0);
    }

    @Override
    public void setCurrentUserId(long userId) {
        mPrefs.edit().putLong(KEY_USER_ID, userId).apply();
    }

    @Override
    public String getToken() {
        return mPrefs.getString(KEY_TOKEN, null);
    }

    @Override
    public void setToken(String token) {
        mPrefs.edit().putString(KEY_TOKEN, token).apply();
    }

    @Override
    public int getCurrentActivityStateId() {
        return mPrefs.getInt(KEY_ACTIVITY_STATE_ID, MainActivityState.Conversations.getId());
    }

    @Override
    public void setCurrentActivityStateId(int id) {
        mPrefs.edit().putInt(KEY_ACTIVITY_STATE_ID, id).apply();
    }

    @Override
    public String getGcmToken() {
        return mPrefs.getString(KEY_GCM_TOKEN, null);
    }

    @Override
    public void setGcmToken(String token) {
        mPrefs.edit().putString(KEY_GCM_TOKEN, token).apply();
    }

    @Override
    public int getUnreadMessagesCount() {
        return mPrefs.getInt(KEY_UNREAD_MESSAGES_COUNT, 0);
    }

    @Override
    public void setUnreadMessagesCount(int count) {
        mPrefs.edit().putInt(KEY_UNREAD_MESSAGES_COUNT, count).apply();
    }

    @Override
    public int getFriendsCount() {
        return mPrefs.getInt(KEY_FRIENDS_COUNT, 0);
    }

    @Override
    public void setFriendsCount(int count) {
        mPrefs.edit().putInt(KEY_FRIENDS_COUNT, count).apply();
    }

    @Override
    public int getKeyboardHeight() {
        return mPrefs.getInt(KEY_KEYBOARD_HEIGHT + "_" + getOrientation(), 0);
    }

    @Override
    public void setKeyboardHeight(int heightPx) {
        mPrefs.edit().putInt(KEY_KEYBOARD_HEIGHT + "_" + getOrientation(), heightPx).apply();
    }

    @Override
    public int getOrientation() {
        return MyApplication.getInstance().getResources().getConfiguration().orientation;
    }

    @Override
    public long getLastFetchStickersTime() {
        return mPrefs.getLong(KEY_FETCH_STICKERS_TIMESTAMP, 0);
    }

    @Override
    public void setLastFetchStickersTime(long timestamp) {
        mPrefs.edit().putLong(KEY_FETCH_STICKERS_TIMESTAMP, timestamp).apply();
    }

    @Override
    public int getCurrentEmojiTab() {
        return mPrefs.getInt(KEY_EMOJI_TAB, 0);
    }

    @Override
    public void setCurrentEmojiTab(int tabNum) {
        mPrefs.edit().putInt(KEY_EMOJI_TAB, tabNum).apply();
    }

    @Override
    public void setMarkMessagesAsRead(boolean markMessagesAsRead) {
        mPrefs.edit().putBoolean(KEY_MARK_MESSAGES_AS_READ, markMessagesAsRead).apply();
    }

    @Override
    public boolean markMessagesAsRead() {
        return mPrefs.getBoolean(KEY_MARK_MESSAGES_AS_READ, true);
    }

    @Override
    public void setDisplayUnreadMessagesAsUnread(boolean accessible) {
        mPrefs.edit().putBoolean(KEY_DISPLAY_UNREAD_AS_UNREAD, accessible).apply();
    }

    @Override
    public boolean isDisplayingUnreadMessagesAsUnread() {
        return mPrefs.getBoolean(KEY_DISPLAY_UNREAD_AS_UNREAD, true);
    }

    @Override
    @DebugLog
    public void addConversationToSyncUnreadMessages(long conversationId, long toTime) {
        Set<LongToLongModel> data = getConversationsToSyncUnreadMessages();
        data.add(new LongToLongModel(conversationId, toTime));
        setLongToLongSet(data, KEY_SYNC_MARK_MESSAGES_TO_READ);
    }

    @Override
    @DebugLog
    public void removeConversationToSyncUnreadMessages(long conversationId, long toTime) {
        LongToLongModel m = new LongToLongModel(conversationId, toTime);
        Set<LongToLongModel> data = getConversationsToSyncUnreadMessages();
        if (data.contains(m)) {
            data.remove(m);
        }
        setLongToLongSet(data, KEY_SYNC_MARK_MESSAGES_TO_READ);
    }

    @Override
    @DebugLog
    public Set<LongToLongModel> getConversationsToSyncUnreadMessages() {
        return getLongToLong(KEY_SYNC_MARK_MESSAGES_TO_READ)
                .toList()
                .map((Func1<List<LongToLongModel>, HashSet<LongToLongModel>>) HashSet::new)
                .toBlocking()
                .first();
    }

    @DebugLog
    private Observable<LongToLongModel> getLongToLong(@NonNull String key) {
        return Observable.from(mPrefs.getStringSet(key, new HashSet<>(0)))
                .subscribeOn(Schedulers.io())
                .map(LongToLongModel::fromString);
    }

    @DebugLog
    private void setLongToLongSet(@NonNull Set<LongToLongModel> data, @NonNull String key) {
        Observable.from(data)
                .subscribeOn(Schedulers.io())
                .map(LongToLongModel::toString)
                .toList()
                .map((Func1<List<String>, HashSet<String>>) HashSet::new)
                .doOnNext(strings -> mPrefs.edit().putStringSet(key, strings))
                .subscribe();
    }

}

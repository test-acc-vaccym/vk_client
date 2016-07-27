package com.nethergrim.vk.adapter;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;

import com.nethergrim.vk.MyApplication;
import com.nethergrim.vk.R;
import com.nethergrim.vk.adapter.viewholders.ConversationViewHolder;
import com.nethergrim.vk.caching.Prefs;
import com.nethergrim.vk.fragment.ConversationsFragment;
import com.nethergrim.vk.models.Conversation;
import com.nethergrim.vk.models.Message;
import com.nethergrim.vk.models.User;
import com.nethergrim.vk.utils.ConversationUtils;
import com.nethergrim.vk.utils.MessageUtils;
import com.nethergrim.vk.utils.UserProvider;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * {@link android.support.v7.widget.RecyclerView.Adapter} that should be used to display list of
 * conversations in {@link ConversationsFragment}
 *
 * @author Andrey Drobyazko (c2q9450@gmail.com).
 *         All rights reserved.
 */
public class ConversationsAdapter extends UltimateAdapter
        implements RealmChangeListener, UltimateAdapter.FooterInterface, View.OnClickListener {

    @Inject
    UserProvider mUserProvider;
    @Inject
    Prefs mPrefs;

    private Realm mRealm;
    private ClickListener clickListener;

    private RealmResults<Conversation> mData;

    private int textColorPrimary;
    private int textColorSecondary;


    public ConversationsAdapter(ClickListener clickListener) {
        super();
        this.clickListener = clickListener;
        MyApplication.getInstance().getMainComponent().inject(this);
        mRealm = Realm.getDefaultInstance();// FIXME: 15.10.15 remove realm from here
        mRealm.setAutoRefresh(true);
        this.mData = mRealm.where(Conversation.class)
                .equalTo("message.deleted", 0)
                .findAllSorted("date", Sort.DESCENDING);
        Context ctx = MyApplication.getInstance();
        textColorPrimary = ctx.getResources().getColor(R.color.primary_text);
        textColorSecondary = ctx.getResources().getColor(R.color.secondary_text);
    }

    @Override
    public void onChange() {
        notifyDataSetChanged();
    }


    public Conversation getData(int position) {
        return mData.get(position);
    }

    @Override
    public int getDataSize() {
        return mData.size();
    }

    @Override
    public int getDataViewResId(int viewType) {
        return R.layout.vh_conversation;
    }

    @Override
    public long getDataId(int dataPosition) {
        return mData.get(dataPosition).getId();
    }

    @Override
    public int getDataViewType(int dataPosition) {
        return 1;
    }

    @Override
    public DataVH getDataViewHolder(View v, int dataViewType) {
        return new ConversationViewHolder(v);
    }

    @Override
    public void bindDataVH(DataVH vh, int i) {
        ConversationViewHolder conversationViewHolder = (ConversationViewHolder) vh;
        conversationViewHolder.itemView.setTag(i);
        conversationViewHolder.itemView.setOnClickListener(this);
        Conversation conversation = mData.get(i);
        Message message = conversation.getMessage();
        String details = "";
        User user;
        Context ctx = conversationViewHolder.itemView.getContext();
        if (message == null) {
            return;
        }

        if (MessageUtils.isMessageWithPhoto(message)) {
            details = "[ " + ctx.getString(R.string.photo) + " ] " + details;
        }
        if (MessageUtils.isMessageWithAudio(message)) {
            details = "[ " + ctx.getString(R.string.audio) + " ] " + details;
        }
        if (MessageUtils.isMessageWithWall(message)) {
            details = "[ " + ctx.getString(R.string.wall_entry) + " ] " + details;
        }
        if (MessageUtils.isMessageWithReply(message)) {
            details = "[ " + ctx.getString(R.string.reply) + " ] " + details;
        }
        if (MessageUtils.isMessageWithDoc(message)) {
            details = "[ " + ctx.getString(R.string.document) + " ] " + details;
        }
        if (MessageUtils.isMessageWithVideo(message)) {
            details = "[ " + ctx.getString(R.string.video) + " ] " + details;
        }

        if (ConversationUtils.isConversationAGroupChat(conversation)) {

            //            group chat
            conversationViewHolder.imageAvatar.displayGroupChat();
            conversationViewHolder.textName.setText(message.getTitle());

            if (ConversationUtils.isMessageFromMe(message)) {
                details = conversationViewHolder.itemView.getResources()
                        .getString(R.string.me_) + " " + details
                        + " " + message.getBody();
            } else {
                user = mUserProvider.getUser(message.getUser_id());
                if (user != null) {
                    details = user.getFirstName() + ": " + details + message.getBody();
                }

            }

            conversationViewHolder.mOnlineIndicator.setVisibility(View.GONE);
        } else {
            //              regular chat
            user = mUserProvider.getUser(conversation.getId());

            details = details + message.getBody();
            if (ConversationUtils.isMessageFromMe(message)) {
                details = conversationViewHolder.itemView.getResources().getString(R.string.me_)
                        + " " + details;
            }

            if (user != null) {
                conversationViewHolder.mOnlineIndicator.setVisibility(
                        user.getOnline() == 1 ? View.VISIBLE : View.GONE);
                conversationViewHolder.imageAvatar.display(user, true);
                conversationViewHolder.textName.setText(
                        user.getFirstName() + " " + user.getLastName());
            }
        }
        if (MessageUtils.isMessageWithSticker(message)) {
            details = details + "[ " + ctx.getString(R.string.sticker) + " ]";
            conversationViewHolder.mImageViewDetails.setVisibility(View.VISIBLE);
            String url = MessageUtils.getStickerFromMessage(message).getPhoto256();
            conversationViewHolder.mImageViewDetails.setImageURI(Uri.parse(url));
        } else {
            conversationViewHolder.mImageViewDetails.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(details)) {
            conversationViewHolder.textDetails.setText(details);
            conversationViewHolder.textDetails.setVisibility(View.VISIBLE);
        } else {
            conversationViewHolder.textDetails.setVisibility(View.GONE);
        }

        conversationViewHolder.textDate.setText(
                DateUtils.getRelativeTimeSpanString(message.getDate() * 1000,
                        System.currentTimeMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL));
        if (ConversationUtils.isConversationUnread(conversation)) {
            conversationViewHolder.textName.setTextColor(textColorPrimary);
            conversationViewHolder.textDetails.setTextColor(textColorPrimary);
        } else {
            conversationViewHolder.textName.setTextColor(textColorSecondary);
            conversationViewHolder.textDetails.setTextColor(textColorSecondary);
        }
    }

    @Override
    public FooterVH getFooterVH(View v) {
        return new MyFooterVH(v);
    }

    @Override
    public int getFooterViewResId() {
        return R.layout.spinner;
    }

    @Override
    public void bindFooterVH(FooterVH vh) {
        // nothing to bind
    }

    @Override
    public void onClick(View v) {
        int index = (int) v.getTag();
        clickListener.onConversationClicked(index, mData.get(index));
    }

    public interface ClickListener {
        void onConversationClicked(int index, Conversation conversation);
    }

    public static class MyFooterVH extends FooterVH {


        public MyFooterVH(View itemView) {
            super(itemView);
        }
    }
}

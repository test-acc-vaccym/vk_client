package com.nethergrim.vk.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nethergrim.vk.MyApplication;
import com.nethergrim.vk.R;
import com.nethergrim.vk.activity.ChatActivity;
import com.nethergrim.vk.activity.NewChatActivity;
import com.nethergrim.vk.adapter.ConversationsAdapter;
import com.nethergrim.vk.callbacks.ToolbarScrollable;
import com.nethergrim.vk.event.ConversationsUpdatedEvent;
import com.nethergrim.vk.models.Conversation;
import com.nethergrim.vk.utils.BasicRecyclerViewScroller;
import com.nethergrim.vk.utils.FabAnimationManager;
import com.nethergrim.vk.utils.RecyclerItemClickListener;
import com.nethergrim.vk.views.PaginationManager;
import com.nethergrim.vk.web.WebIntentHandler;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.realm.Sort;


/**
 * @author Andrew Drobyazko - c2q9450@gmail.com - https://nethergrim.github.io on 3/20/15.
 */
public class ConversationsFragment extends AbstractFragment
        implements PaginationManager.OnRecyclerViewScrolledToPageListener, ToolbarScrollable,
        RecyclerItemClickListener.OnItemClickListener, ConversationsAdapter.ClickListener {

    private static final int DEFAULT_PAGE_SIZE = 190;
    @InjectView(R.id.list)
    RecyclerView mRecyclerView;
    @InjectView(R.id.progressBar2)
    ProgressBar mProgressBar;
    @InjectView(R.id.textViewNothingHere)
    TextView mNothingHereTextView;
    @Inject
    WebIntentHandler mWebIntentHandler;
    @Inject
    Bus mBus;
    @InjectView(R.id.fab_normal)
    FloatingActionButton mFabNormal;
    @InjectView(R.id.progressBottom)
    ProgressBar mProgressBottom;
    private ConversationsAdapter mAdapter;

    private ToolbarScrollable mToolbarScrollable;
    private FabAnimationManager mFabAnimationManager;


    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ToolbarScrollable) {
            mToolbarScrollable = (ToolbarScrollable) activity;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        MyApplication.getInstance().getMainComponent().inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        mBus.register(this);
        View v = inflater.inflate(R.layout.fragment_messages, container, false);
        ButterKnife.inject(this, v);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFabAnimationManager = new FabAnimationManager(mFabNormal);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mRecyclerView.setHasFixedSize(true);

        List<Conversation> data = mRealm.where(Conversation.class)
                .equalTo("message.deleted", 0)
                .findAllSorted("date", Sort.DESCENDING);


        mAdapter = new ConversationsAdapter(this, data);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new PaginationManager(DEFAULT_PAGE_SIZE, this,
                DEFAULT_PAGE_SIZE / 2));
        mRecyclerView.addOnScrollListener(new BasicRecyclerViewScroller(this));
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(view.getContext(), this));
        if (mAdapter.getItemCount() == 0) {
            mProgressBar.setVisibility(View.VISIBLE);
            mNothingHereTextView.setVisibility(View.GONE);
        }
        loadPage(0);
    }

    @Override
    public void onChange(Object element) {
        super.onChange(element);
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
        mBus.unregister(this);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mToolbarScrollable = null;
    }

    @Override
    public void onRecyclerViewScrolledToPage(int pageNumber) {
        loadPage(pageNumber - 1);
    }

    @Subscribe
    public void onDataUpdated(ConversationsUpdatedEvent event) {
        mProgressBottom.setVisibility(View.GONE);
        mAdapter.setFooterVisibility(View.GONE);
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
            if (mAdapter.getItemCount() == 0) {
                mProgressBar.setVisibility(View.GONE);
                mNothingHereTextView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void showToolbar() {
        if (mFabNormal != null) {
            mFabAnimationManager.showFab();
        }
        if (mToolbarScrollable != null) {
            mToolbarScrollable.showToolbar();
        }
    }


    @Override
    public void hideToolbar() {
        if (mFabNormal != null) {
            mFabAnimationManager.hideFab();
        }
        if (mToolbarScrollable != null) {
            mToolbarScrollable.hideToolbar();
        }
    }

    @OnClick(R.id.fab_normal)
    public void onFabClicked(View v) {
        NewChatActivity.start(v.getContext());
    }


    @Override
    public void onConversationClicked(int index, Conversation conversation) {
        ChatActivity.start(getActivity(), conversation);
    }

    @Override
    public void onItemClick(View childView, int position) {

    }

    @Override
    public void onItemLongPress(View childView, int position) {
        Conversation conversation = mAdapter.getData(position);
        Context ctx = childView.getContext();

        Dialog dialog = new AlertDialog.Builder(ctx)
                .setTitle(R.string.delete_chat_with)
                .setMessage(R.string.are_you_sure)
                .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                    mWebIntentHandler.deleteConversation(conversation);
                    dialogInterface.dismiss();
                })
                .setNegativeButton(R.string.no, (dialogInterface, i) -> dialogInterface.dismiss())
                .create();
        dialog.show();
    }

    private void loadPage(int pageNumber) {
        if (pageNumber == 0) {
            mProgressBottom.setVisibility(View.VISIBLE);
        }
        mAdapter.setFooterVisibility(View.VISIBLE);
        mWebIntentHandler.fetchConversationsAndUsers(DEFAULT_PAGE_SIZE,
                pageNumber * DEFAULT_PAGE_SIZE, false);
    }
}

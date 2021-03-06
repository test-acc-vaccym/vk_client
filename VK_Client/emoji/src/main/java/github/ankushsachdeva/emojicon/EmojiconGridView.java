/*
 * Copyright 2014 Ankush Sachdeva
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package github.ankushsachdeva.emojicon;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageButton;

import java.util.Arrays;

import github.ankushsachdeva.emojicon.emoji.Emojicon;
import github.ankushsachdeva.emojicon.emoji.People;

/**
 * @author Hieu Rocker (rockerhieu@gmail.com)
 * @author Ankush Sachdeva (sankush@yahoo.co.in)
 */
public class EmojiconGridView {

    public View rootView;

    Emojicon[] mData;

    public interface OnEmojiconClickedListener {

        void onEmojiconClicked(Emojicon emojicon);

        void onEmojiconBackPressClicked();

        void onStickerClicked(long stickerId);
    }

    public EmojiconGridView(Context context,
            final OnEmojiconClickedListener callback) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Activity.LAYOUT_INFLATER_SERVICE);
        rootView = inflater.inflate(R.layout.emojicon_grid, null);

        GridView gridView = (GridView) rootView.findViewById(R.id.Emoji_GridView);
        Object[] emojicons = Emojicon.DATA;
        if (emojicons == null) {
            mData = People.DATA;
        } else {
            mData = Arrays.asList(emojicons).toArray(new Emojicon[emojicons.length]);
        }
        EmojiAdapter mAdapter = new EmojiAdapter(rootView.getContext(), mData);
        mAdapter.setClickedListener(callback);
        gridView.setAdapter(mAdapter);

        ImageButton deleteButton = (ImageButton) rootView.findViewById(R.id.btn_delete);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.onEmojiconBackPressClicked();
                }
            }
        });
    }

    public View getRootView() {
        return rootView;
    }

}

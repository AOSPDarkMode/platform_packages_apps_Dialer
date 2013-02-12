/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dialer.dialpad;

import android.content.Context;
import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

import com.android.dialer.R;
import com.google.common.collect.Lists;

import java.util.List;

public class SmartDialAdapter extends BaseAdapter {
    public static final String LOG_TAG = "SmartDial";
    private final LayoutInflater mInflater;

    private List<SmartDialEntry> mEntries;

    private final int mHighlightedTextColor;

    public SmartDialAdapter(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final Resources res = context.getResources();
        mHighlightedTextColor = res.getColor(R.color.smartdial_highlighted_text_color);
        clear();
    }

    /** Remove all entries. */
    public void clear() {
        mEntries = Lists.newArrayList();
        notifyDataSetChanged();
    }

    /** Set entries. */
    public void setEntries(List<SmartDialEntry> entries) {
        if (entries == null) throw new IllegalArgumentException();
        mEntries = entries;

        if (mEntries.size() <= 1) {
            // add a null entry to push the single entry into the middle
            mEntries.add(0, null);
        } else if (mEntries.size() >= 2){
            // swap the 1st and 2nd entries so that the highest confidence match goes into the
            // middle
            final SmartDialEntry temp = mEntries.get(0);
            mEntries.set(0, mEntries.get(1));
            mEntries.set(1, temp);
        }

        notifyDataSetChanged();
    }

    @Override
    public boolean isEnabled(int position) {
        return !(mEntries.get(position) == null);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public int getCount() {
        return mEntries.size();
    }

    @Override
    public Object getItem(int position) {
        return mEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position; // Just use the position as the ID, so it's not stable.
    }

    @Override
    public boolean hasStableIds() {
        return false; // Not stable because we just use the position as the ID.
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final LinearLayout view;
        if (convertView == null) {
            view = (LinearLayout) mInflater.inflate(
                    R.layout.dialpad_smartdial_item, parent, false);
        } else {
            view = (LinearLayout) convertView;
        }

        final SmartDialTextView nameView = (SmartDialTextView) view.findViewById(R.id.contact_name);

        final SmartDialTextView numberView = (SmartDialTextView) view.findViewById(
                R.id.contact_number);

        final SmartDialEntry item = mEntries.get(position);

        if (item == null) {
            // Clear the text in case the view was reused.
            nameView.setText("");
            numberView.setText("");
            // Empty view. We use this to force a single entry to be in the middle
            return view;
        }

        // Highlight the display name with the provided match positions
        if (!TextUtils.isEmpty(item.displayName)) {
            final SpannableString displayName = new SpannableString(item.displayName);
            for (final SmartDialMatchPosition p : item.matchPositions) {
                if (p.start < p.end) {
                    if (p.end > displayName.length()) {
                        p.end = displayName.length();
                    }
                    // Create a new ForegroundColorSpan for each section of the name to highlight,
                    // otherwise multiple highlights won't work.
                    displayName.setSpan(new ForegroundColorSpan(mHighlightedTextColor), p.start,
                            p.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            nameView.setText(displayName);
        }

        // Highlight the phone number with the provided match positions
        if (!TextUtils.isEmpty(item.phoneNumber)) {
            final SmartDialMatchPosition p = item.phoneNumberMatchPosition;
            final SpannableString phoneNumber = new SpannableString(item.phoneNumber);
            if (p != null && p.start < p.end) {
                if (p.end > phoneNumber.length()) {
                    p.end = phoneNumber.length();
                }
                phoneNumber.setSpan(new ForegroundColorSpan(mHighlightedTextColor), p.start, p.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            numberView.setText(phoneNumber);
        }
        view.setTag(item);

        return view;
    }
}

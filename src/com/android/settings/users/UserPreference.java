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

package com.android.settings.users;

import com.android.internal.util.CharSequences;
import com.android.settings.R;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;

public class UserPreference extends Preference {

    public static final int USERID_UNKNOWN = -10;

    private OnClickListener mDeleteClickListener;
    private OnClickListener mSettingsClickListener;
    private int mSerialNumber = -1;
    private int mUserId = USERID_UNKNOWN;
    private boolean mRestricted;
    static final int SETTINGS_ID = R.id.manage_user;
    static final int DELETE_ID = R.id.trash_user;

    public UserPreference(Context context, AttributeSet attrs) {
        this(context, attrs, USERID_UNKNOWN, false, null, null);
    }

    UserPreference(Context context, AttributeSet attrs, int userId,
            boolean showOptions, OnClickListener deleteListener,
            OnClickListener settingsListener) {
        super(context, attrs);
        if (showOptions) {
            setWidgetLayoutResource(R.layout.preference_user_delete_widget);
            mDeleteClickListener = deleteListener;
            mSettingsClickListener = settingsListener;
        }
        mUserId = userId;
        if (mUserId > UserHandle.USER_OWNER) {
            mRestricted = ((UserManager) getContext().getSystemService(Context.USER_SERVICE))
                    .getUserInfo(mUserId).isRestricted();
        }
        //setSummary(mRestricted ? R.string.user_limited : R.string.user_trusted);
    }

    @Override
    protected void onBindView(View view) {
        View deleteView = view.findViewById(R.id.trash_user);
        if (deleteView != null) {
            deleteView.setOnClickListener(mDeleteClickListener);
            deleteView.setTag(this);
        }
        View settingsView = view.findViewById(R.id.manage_user);
        if (settingsView != null) {
            if (mRestricted) {
                settingsView.setOnClickListener(mSettingsClickListener);
                settingsView.setTag(this);
            } else {
                settingsView.setVisibility(View.INVISIBLE);
            }
        }
        super.onBindView(view);
    }

    public int getSerialNumber() {
        if (mSerialNumber < 0) {
            // If the userId is unknown
            if (mUserId == USERID_UNKNOWN) return Integer.MAX_VALUE;
            mSerialNumber = ((UserManager) getContext().getSystemService(Context.USER_SERVICE))
                    .getUserSerialNumber(mUserId);
            if (mSerialNumber < 0) return mUserId;
        }
        return mSerialNumber;
    }

    public int getUserId() {
        return mUserId;
    }

    public int compareTo(Preference another) {
        if (another instanceof UserPreference) {
            return getSerialNumber() > ((UserPreference) another).getSerialNumber() ? 1 : -1;
        } else {
            return 1;
        }
    }
}

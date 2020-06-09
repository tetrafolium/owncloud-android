package com.owncloud.android.datamodel;

import androidx.annotation.Nullable;

/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class UserProfile {

  private long mId;
  private String mAccountName;

  private String mUserId;
  private String mDisplayName = "";
  private String mEmail = "";

  private UserAvatar mAvatar;
  private UserQuota mQuota;

  public UserProfile(final String accountName, final String userId,
                     final String displayName, final String email) {
    mAccountName = accountName;
    mUserId = userId;
    mDisplayName = displayName;
    mEmail = email;
    mAvatar = null;
  }

  public String getAccountName() { return mAccountName; }

  public String getUserId() { return mUserId; }

  public String getDisplayName() { return mDisplayName; }

  public String getEmail() { return mEmail; }

  @Nullable
  public UserAvatar getAvatar() {
    return mAvatar;
  }

  public void setAvatar(final UserAvatar avatar) { mAvatar = avatar; }

  @Nullable
  public UserQuota getQuota() {
    return mQuota;
  }

  public void setQuota(final UserQuota quota) { this.mQuota = quota; }

  public static class UserAvatar {

    private String mCacheKey;
    private String mMimeType;
    private String mEtag;

    public UserAvatar(final String cacheKey, final String mimeType,
                      final String etag) {
      mCacheKey = cacheKey;
      mMimeType = mimeType;
      mEtag = etag;
    }

    public String getCacheKey() { return mCacheKey; }

    public String getMimeType() { return mMimeType; }

    public String getEtag() { return mEtag; }
  }

  public static class UserQuota {

    private long mFree;
    private double mRelative;
    private long mTotal;
    private long mUsed;

    public UserQuota(final long free, final double relative, final long total,
                     final long used) {
      mFree = free;
      mRelative = relative;
      mTotal = total;
      mUsed = used;
    }

    public long getFree() { return mFree; }

    public double getRelative() { return mRelative; }

    public long getTotal() { return mTotal; }

    public long getUsed() { return mUsed; }
  }
}

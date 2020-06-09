/**
 * ownCloud Android client application
 *
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

package com.owncloud.android.datamodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.owncloud.android.MainApp;
import com.owncloud.android.db.ProviderMeta;
import java.io.File;
import timber.log.Timber;

/**
 * Minimum to get things working.
 *
 * Working around FileContentProvider, we have no interest in exporting user
 * profiles to other apps.
 */
public class UserProfilesRepository {

  private static UserProfilesRepository sUserProfilesSingleton;

  private static SQLiteDatabase database;

  private UserProfilesRepository() {}

  public static UserProfilesRepository getUserProfilesRepository() {
    if (sUserProfilesSingleton == null) {
      sUserProfilesSingleton = new UserProfilesRepository();
    }
    return sUserProfilesSingleton;
  }

  /**
   * Persist a user profile.
   *
   * Minimum to get things working: only storing info about avatar.
   *
   * Working around ContentProvider
   *
   * @param userProfile           User profile.
   */
  public void update(final UserProfile userProfile) {

    SQLiteDatabase database = getSqLiteDatabase();

    if (userProfile == null) {
      throw new IllegalArgumentException(
          "Received userProfile with NULL value");
    }

    if (userProfile.getAvatar() != null) {
      // map avatar properties to columns
      ContentValues avatarValues = new ContentValues();
      avatarValues.put(
          ProviderMeta.ProviderTableMeta.USER_AVATARS__ACCOUNT_NAME,
          userProfile.getAccountName());
      avatarValues.put(ProviderMeta.ProviderTableMeta.USER_AVATARS__CACHE_KEY,
                       userProfile.getAvatar().getCacheKey());
      avatarValues.put(ProviderMeta.ProviderTableMeta.USER_AVATARS__ETAG,
                       userProfile.getAvatar().getEtag());
      avatarValues.put(ProviderMeta.ProviderTableMeta.USER_AVATARS__MIME_TYPE,
                       userProfile.getAvatar().getMimeType());

      database.beginTransaction();
      try {
        if (avatarExists(userProfile)) {
          // not new, UPDATE
          database.update(
              ProviderMeta.ProviderTableMeta.USER_AVATARS__TABLE_NAME,
              avatarValues,
              ProviderMeta.ProviderTableMeta.USER_AVATARS__ACCOUNT_NAME + "=?",
              new String[] {String.valueOf(userProfile.getAccountName())});
          Timber.d("Avatar updated");

        } else {
          // new, CREATE
          database.insert(
              ProviderMeta.ProviderTableMeta.USER_AVATARS__TABLE_NAME, null,
              avatarValues);
          Timber.d("Avatar inserted");
        }
        database.setTransactionSuccessful();

      } finally {
        database.endTransaction();
      }
    }

    if (userProfile.getQuota() != null) {
      // map quota properties to columns
      ContentValues quotaValues = new ContentValues();
      quotaValues.put(ProviderMeta.ProviderTableMeta.USER_QUOTAS__ACCOUNT_NAME,
                      userProfile.getAccountName());
      quotaValues.put(ProviderMeta.ProviderTableMeta.USER_QUOTAS__FREE,
                      userProfile.getQuota().getFree());
      quotaValues.put(ProviderMeta.ProviderTableMeta.USER_QUOTAS__RELATIVE,
                      userProfile.getQuota().getRelative());
      quotaValues.put(ProviderMeta.ProviderTableMeta.USER_QUOTAS__TOTAL,
                      userProfile.getQuota().getTotal());
      quotaValues.put(ProviderMeta.ProviderTableMeta.USER_QUOTAS__USED,
                      userProfile.getQuota().getUsed());

      database.beginTransaction();
      try {
        if (quotaExists(userProfile)) {
          // not new, UPDATE
          database.update(
              ProviderMeta.ProviderTableMeta.USER_QUOTAS_TABLE_NAME,
              quotaValues,
              ProviderMeta.ProviderTableMeta.USER_QUOTAS__ACCOUNT_NAME + "=?",
              new String[] {String.valueOf(userProfile.getAccountName())});
          Timber.d("Quota updated");

        } else {
          // new, CREATE
          database.insert(ProviderMeta.ProviderTableMeta.USER_QUOTAS_TABLE_NAME,
                          null, quotaValues);
          Timber.d("Quota inserted");
        }
        database.setTransactionSuccessful();

      } finally {
        database.endTransaction();
      }
    }
  }

  /**
   * Gets the information about a user avatar bound to an OC account.
   *
   * Shortcut method prevent retrieving a full {@link UserProfile},
   * specially now that {@link UserProfile}s are not really stored. Naughty
   * trick.
   *
   * @param   accountName         Name of an OC account.
   * @return Information about a user avatar bound to an OC account, or NULL if
   *                              there is no avatar for the given account.
   */
  public UserProfile.UserAvatar getAvatar(final String accountName) {
    UserProfile.UserAvatar avatar = null;
    Cursor c = null;

    try {
      c = getSqLiteDatabase().query(
          ProviderMeta.ProviderTableMeta.USER_AVATARS__TABLE_NAME, null,
          ProviderMeta.ProviderTableMeta.USER_AVATARS__ACCOUNT_NAME + "=?",
          new String[] {accountName}, null, null, null);
      if (c != null && c.moveToFirst()) {
        avatar = new UserProfile.UserAvatar(
            c.getString(c.getColumnIndex(
                ProviderMeta.ProviderTableMeta.USER_AVATARS__CACHE_KEY)),
            c.getString(c.getColumnIndex(
                ProviderMeta.ProviderTableMeta.USER_AVATARS__MIME_TYPE)),
            c.getString(c.getColumnIndex(
                ProviderMeta.ProviderTableMeta.USER_AVATARS__ETAG)));
      } // else, no avatar to return
    } catch (Exception e) {
      Timber.e(e, "Exception while querying avatar");
    } finally {
      if (c != null) {
        c.close();
      }
    }
    return avatar;
  }

  public UserProfile.UserQuota getQuota(final String accountName) {
    UserProfile.UserQuota userQuota = null;
    Cursor c = null;
    try {
      c = getSqLiteDatabase().query(
          ProviderMeta.ProviderTableMeta.USER_QUOTAS_TABLE_NAME, null,
          ProviderMeta.ProviderTableMeta.USER_QUOTAS__ACCOUNT_NAME + "=?",
          new String[] {accountName}, null, null, null);
      if (c != null && c.moveToFirst()) {

        userQuota = new UserProfile.UserQuota(
            c.getLong(c.getColumnIndex(
                ProviderMeta.ProviderTableMeta.USER_QUOTAS__FREE)),
            c.getDouble(c.getColumnIndex(
                ProviderMeta.ProviderTableMeta.USER_QUOTAS__RELATIVE)),
            c.getLong(c.getColumnIndex(
                ProviderMeta.ProviderTableMeta.USER_QUOTAS__TOTAL)),
            c.getLong(c.getColumnIndex(
                ProviderMeta.ProviderTableMeta.USER_QUOTAS__USED)));
      }
    } catch (Exception e) {
      Timber.e(e, "Exception while querying quota");
    } finally {
      if (c != null) {
        c.close();
      }
    }
    return userQuota;
  }

  public void deleteAvatar(final String accountName) {
    try {
      getSqLiteDatabase().delete(
          ProviderMeta.ProviderTableMeta.USER_AVATARS__TABLE_NAME,
          ProviderMeta.ProviderTableMeta.USER_AVATARS__ACCOUNT_NAME + "=?",
          new String[] {String.valueOf(accountName)});
      Timber.d("Avatar deleted");

    } catch (Exception e) {
      Timber.e(e, "Exception while deleting avatar");
    }
  }

  private boolean avatarExists(final UserProfile userProfile) {
    boolean exists;
    Cursor c = null;
    try {
      c = getSqLiteDatabase().query(
          ProviderMeta.ProviderTableMeta.USER_AVATARS__TABLE_NAME, null,
          ProviderMeta.ProviderTableMeta.USER_AVATARS__ACCOUNT_NAME + "=?",
          new String[] {userProfile.getAccountName()}, null, null, null);
      exists = (c != null && c.moveToFirst());
    } finally {
      if (c != null) {
        c.close();
      }
    }
    return exists;
  }

  private boolean quotaExists(final UserProfile userProfile) {
    boolean exists;
    Cursor c = null;
    try {
      c = getSqLiteDatabase().query(
          ProviderMeta.ProviderTableMeta.USER_QUOTAS_TABLE_NAME, null,
          ProviderMeta.ProviderTableMeta.USER_QUOTAS__ACCOUNT_NAME + "=?",
          new String[] {userProfile.getAccountName()}, null, null, null);
      exists = (c != null && c.moveToFirst());
    } finally {
      if (c != null) {
        c.close();
      }
    }
    return exists;
  }

  /**
   * Open and retrieve a SQL Lite database
   *
   * @return SQL Lite database
   */
  private SQLiteDatabase getSqLiteDatabase() {

    File dbFile =
        MainApp.Companion.getAppContext().getDatabasePath(ProviderMeta.DB_NAME);

    if (database == null) {

      database = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null,
                                             SQLiteDatabase.OPEN_READWRITE);
    }

    return database;
  }
}

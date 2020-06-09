package com.owncloud.android.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import androidx.appcompat.app.AppCompatDialog;
import java.lang.reflect.Method;
import timber.log.Timber;

public class OwnCloudListPreference extends ListPreference {

  private Context mContext;
  private AppCompatDialog mDialog;

  public OwnCloudListPreference(final Context context) {
    super(context);
    this.mContext = context;
  }

  public OwnCloudListPreference(final Context context,
                                final AttributeSet attrs) {
    super(context, attrs);
    this.mContext = context;
  }

  public OwnCloudListPreference(final Context context, final AttributeSet attrs,
                                final int defStyleAttr, final int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  public OwnCloudListPreference(final Context context, final AttributeSet attrs,
                                final int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void showDialog(final Bundle state) {
    if (getEntries() == null || getEntryValues() == null) {
      throw new IllegalStateException(
          "ListPreference requires an entries array and an entryValues array.");
    }

    int preselect = findIndexOfValue(getValue());

    // same thing happens for the Standard ListPreference though
    androidx.appcompat.app.AlertDialog.Builder builder =
        new androidx.appcompat.app.AlertDialog.Builder(mContext)
            .setTitle(getDialogTitle())
            .setIcon(getDialogIcon())
            .setSingleChoiceItems(getEntries(), preselect, this);

    PreferenceManager pm = getPreferenceManager();
    try {
      Method method = pm.getClass().getDeclaredMethod(
          "registerOnActivityDestroyListener",
          PreferenceManager.OnActivityDestroyListener.class);
      method.setAccessible(true);
      method.invoke(pm, this);
    } catch (Exception e) {
      // no way to handle this but logging it
      Timber.e(e, "error invoking registerOnActivityDestroyListener");
    }

    mDialog = builder.create();
    if (state != null) {
      mDialog.onRestoreInstanceState(state);
    }
    mDialog.show();
  }

  @Override
  public void onClick(final DialogInterface dialog, final int which) {
    if (which >= 0 && getEntryValues() != null) {
      String value = getEntryValues()[which].toString();
      if (callChangeListener(value)) {
        setValue(value);
      }
      dialog.dismiss();
    }
  }

  @Override
  public AppCompatDialog getDialog() {
    return mDialog;
  }

  @Override
  public void onActivityDestroy() {
    super.onActivityDestroy();
    if (mDialog != null && mDialog.isShowing()) {
      mDialog.dismiss();
    }
  }
}

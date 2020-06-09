/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
 * @author Abel García de Prada
 * @author Shashvat Kedia
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
package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.media.MediaControlView;
import com.owncloud.android.media.MediaService;
import com.owncloud.android.media.MediaServiceBinder;
import com.owncloud.android.ui.controller.TransferProgressController;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.PreferenceUtils;
import timber.log.Timber;

/**
 * This fragment shows a preview of a downloaded audio.
 * <p>
 * Trying to get an instance with NULL {@link OCFile} or ownCloud {@link
 * Account} values will produce an {@link IllegalStateException}. <p> If the
 * {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is
 * generated on instantiation too.
 */
public class PreviewAudioFragment extends FileFragment {

  public static final String EXTRA_FILE = "FILE";
  public static final String EXTRA_ACCOUNT = "ACCOUNT";
  private static final String EXTRA_PLAY_POSITION = "PLAY_POSITION";
  private static final String EXTRA_PLAYING = "PLAYING";

  private Account mAccount;
  private ImageView mImagePreview;
  private int mSavedPlaybackPosition;

  private MediaServiceBinder mMediaServiceBinder = null;
  private MediaControlView mMediaController = null;
  private MediaServiceConnection mMediaServiceConnection = null;
  private boolean mAutoplay;

  private ProgressBar mProgressBar = null;
  public TransferProgressController mProgressController;

  /**
   * Public factory method to create new PreviewAudioFragment instances.
   *
   * @param file                  An {@link OCFile} to preview in the fragment
   * @param account               ownCloud account containing file
   * @param startPlaybackPosition Time in milliseconds where the play should be
   *     started
   * @param autoplay              If 'true', the file will be played
   *     automatically when
   *                              the fragment is displayed.
   * @return Fragment ready to be used.
   */
  public static PreviewAudioFragment
  newInstance(final OCFile file, final Account account,
              final int startPlaybackPosition, final boolean autoplay) {
    PreviewAudioFragment frag = new PreviewAudioFragment();
    Bundle args = new Bundle();
    args.putParcelable(EXTRA_FILE, file);
    args.putParcelable(EXTRA_ACCOUNT, account);
    args.putInt(EXTRA_PLAY_POSITION, startPlaybackPosition);
    args.putBoolean(EXTRA_PLAYING, autoplay);
    frag.setArguments(args);
    return frag;
  }

  /**
   * Creates an empty fragment for preview audio files.
   * MUST BE KEPT: the system uses it when tries to reinstantiate a fragment
   * automatically (for instance, when the device is turned a aside). DO NOT
   * CALL IT: an {@link OCFile} and {@link Account} must be provided for a
   * successful construction
   */
  public PreviewAudioFragment() {
    super();
    mAccount = null;
    mSavedPlaybackPosition = 0;
    mAutoplay = true;
    mProgressController = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public View onCreateView(final LayoutInflater inflater,
                           final ViewGroup container,
                           final Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    Timber.v("onCreateView");

    View view =
        inflater.inflate(R.layout.preview_audio_fragment, container, false);
    view.setFilterTouchesWhenObscured(
        PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(
            getContext()));

    mImagePreview = view.findViewById(R.id.image_preview);
    mMediaController = view.findViewById(R.id.media_controller);
    mProgressBar = view.findViewById(R.id.syncProgressBar);

    return view;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onActivityCreated(final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Timber.v("onActivityCreated");

    OCFile file;
    Bundle args = getArguments();
    if (savedInstanceState == null) {
      file = args.getParcelable(PreviewAudioFragment.EXTRA_FILE);
      setFile(file);
      mAccount = args.getParcelable(PreviewAudioFragment.EXTRA_ACCOUNT);
      mSavedPlaybackPosition =
          args.getInt(PreviewAudioFragment.EXTRA_PLAY_POSITION);
      mAutoplay = args.getBoolean(PreviewAudioFragment.EXTRA_PLAYING);

    } else {
      file = savedInstanceState.getParcelable(PreviewAudioFragment.EXTRA_FILE);
      setFile(file);
      mAccount =
          savedInstanceState.getParcelable(PreviewAudioFragment.EXTRA_ACCOUNT);
      mSavedPlaybackPosition = savedInstanceState.getInt(
          PreviewAudioFragment.EXTRA_PLAY_POSITION,
          args.getInt(PreviewAudioFragment.EXTRA_PLAY_POSITION));
      mAutoplay = savedInstanceState.getBoolean(
          PreviewAudioFragment.EXTRA_PLAYING,
          args.getBoolean(PreviewAudioFragment.EXTRA_PLAYING));
    }

    if (file == null) {
      throw new IllegalStateException("Instanced with a NULL OCFile");
    }
    if (mAccount == null) {
      throw new IllegalStateException("Instanced with a NULL ownCloud Account");
    }
    if (!file.isDown()) {
      throw new IllegalStateException("There is no local file to preview");
    }
    if (!file.isAudio()) {
      throw new IllegalStateException("Not an audio file");
    }

    extractAndSetCoverArt(file);

    mProgressController = new TransferProgressController(mContainerActivity);
    mProgressController.setProgressBar(mProgressBar);
  }

  /**
   * tries to read the cover art from the audio file and sets it as cover art.
   *
   * @param file audio file with potential cover art
   */
  private void extractAndSetCoverArt(final OCFile file) {
    try {
      MediaMetadataRetriever mmr = new MediaMetadataRetriever();
      mmr.setDataSource(file.getStoragePath());
      byte[] data = mmr.getEmbeddedPicture();
      if (data != null) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        mImagePreview.setImageBitmap(bitmap); // associated cover art in bitmap
      } else {
        mImagePreview.setImageResource(R.drawable.logo);
      }
    } catch (Throwable t) {
      mImagePreview.setImageResource(R.drawable.logo);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSaveInstanceState(final Bundle outState) {
    super.onSaveInstanceState(outState);
    Timber.v("onSaveInstanceState");

    outState.putParcelable(PreviewAudioFragment.EXTRA_FILE, getFile());
    outState.putParcelable(PreviewAudioFragment.EXTRA_ACCOUNT, mAccount);
    if (mMediaServiceBinder != null) {
      outState.putInt(PreviewAudioFragment.EXTRA_PLAY_POSITION,
                      mMediaServiceBinder.getCurrentPosition());
      outState.putBoolean(PreviewAudioFragment.EXTRA_PLAYING,
                          mMediaServiceBinder.isPlaying());
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    Timber.v("onStart");

    OCFile file = getFile();
    if (file != null && file.isDown()) {
      bindMediaService();
    }

    mProgressController.startListeningProgressFor(getFile(), mAccount);
  }

  @Override
  public void onTransferServiceConnected() {
    if (mProgressController != null) {
      mProgressController.startListeningProgressFor(getFile(), mAccount);
    }
  }

  @Override
  public void onFileMetadataChanged(final OCFile updatedFile) {
    if (updatedFile != null) {
      setFile(updatedFile);
    }
    getActivity().invalidateOptionsMenu();
  }

  @Override
  public void onFileMetadataChanged() {
    FileDataStorageManager storageManager =
        mContainerActivity.getStorageManager();
    if (storageManager != null) {
      setFile(storageManager.getFileByPath(getFile().getRemotePath()));
    }
    getActivity().invalidateOptionsMenu();
  }

  @Override
  public void onFileContentChanged() {
    playAudio(true);
  }

  @Override
  public void updateViewForSyncInProgress() {
    mProgressController.showProgressBar();
  }

  @Override
  public void updateViewForSyncOff() {
    mProgressController.hideProgressBar();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCreateOptionsMenu(final Menu menu,
                                  final MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.file_actions_menu, menu);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onPrepareOptionsMenu(final Menu menu) {
    super.onPrepareOptionsMenu(menu);

    FileMenuFilter mf = new FileMenuFilter(getFile(), mAccount,
                                           mContainerActivity, getActivity());
    mf.filter(menu, false, false, false, false);

    // additional restriction for this fragment
    // TODO allow renaming in PreviewAudioFragment
    MenuItem item = menu.findItem(R.id.action_rename_file);
    if (item != null) {
      item.setVisible(false);
      item.setEnabled(false);
    }

    // additional restriction for this fragment
    item = menu.findItem(R.id.action_move);
    if (item != null) {
      item.setVisible(false);
      item.setEnabled(false);
    }

    // additional restriction for this fragment
    item = menu.findItem(R.id.action_copy);
    if (item != null) {
      item.setVisible(false);
      item.setEnabled(false);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    switch (item.getItemId()) {
    case R.id.action_share_file: {
      mContainerActivity.getFileOperationsHelper().showShareFile(getFile());
      return true;
    }
    case R.id.action_open_file_with: {
      openFile();
      return true;
    }
    case R.id.action_remove_file: {
      RemoveFilesDialogFragment dialog =
          RemoveFilesDialogFragment.newInstance(getFile());
      dialog.show(getFragmentManager(),
                  ConfirmationDialogFragment.FTAG_CONFIRMATION);
      return true;
    }
    case R.id.action_see_details: {
      seeDetails();
      return true;
    }
    case R.id.action_send_file: {
      mContainerActivity.getFileOperationsHelper().sendDownloadedFile(
          getFile());
      return true;
    }
    case R.id.action_sync_file: {
      mContainerActivity.getFileOperationsHelper().syncFile(getFile());
      return true;
    }
    case R.id.action_set_available_offline: {
      mContainerActivity.getFileOperationsHelper().toggleAvailableOffline(
          getFile(), true);
      return true;
    }
    case R.id.action_unset_available_offline: {
      mContainerActivity.getFileOperationsHelper().toggleAvailableOffline(
          getFile(), false);
      return true;
    }
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  private void seeDetails() { mContainerActivity.showDetails(getFile()); }

  @Override
  public void onStop() {
    Timber.v("onStop");

    mProgressController.stopListeningProgressFor(getFile(), mAccount);

    if (mMediaServiceConnection != null) {
      Timber.d("Unbinding from MediaService ...");
      if (mMediaServiceBinder != null && mMediaController != null) {
        mMediaServiceBinder.unregisterMediaController(mMediaController);
      }
      getActivity().unbindService(mMediaServiceConnection);
      mMediaServiceConnection = null;
      mMediaServiceBinder = null;
    }

    super.onStop();
  }

  public void playAudio(final boolean restart) {
    OCFile file = getFile();
    if (restart) {
      Timber.d("restarting playback of %s", file.getStoragePath());
      mAutoplay = true;
      mSavedPlaybackPosition = 0;
      mMediaServiceBinder.start(mAccount, file, true, 0);

    } else if (!mMediaServiceBinder.isPlaying(file)) {
      Timber.d("starting playback of %s", file.getStoragePath());
      mMediaServiceBinder.start(mAccount, file, mAutoplay,
                                mSavedPlaybackPosition);

    } else {
      if (!mMediaServiceBinder.isPlaying() && mAutoplay) {
        mMediaServiceBinder.start();
        mMediaController.updatePausePlay();
      }
    }
  }

  private void bindMediaService() {
    Timber.d("Binding to MediaService...");
    if (mMediaServiceConnection == null) {
      mMediaServiceConnection = new MediaServiceConnection();
      getActivity().bindService(new Intent(getActivity(), MediaService.class),
                                mMediaServiceConnection,
                                Context.BIND_AUTO_CREATE);
      // follow the flow in MediaServiceConnection#onServiceConnected(...)
    }
  }

  /**
   * Defines callbacks for service binding, passed to bindService()
   */
  private class MediaServiceConnection implements ServiceConnection {

    @Override
    public void onServiceConnected(final ComponentName component,
                                   final IBinder service) {
      if (getActivity() != null) {
        if (component.equals(
                new ComponentName(getActivity(), MediaService.class))) {
          Timber.d("Media service connected");
          mMediaServiceBinder = (MediaServiceBinder)service;
          if (mMediaServiceBinder != null) {
            prepareMediaController();
            playAudio(false);
            Timber.d(
                "Successfully bound to MediaService, MediaController ready");

          } else {
            Timber.e("Unexpected response from MediaService while binding");
          }
        }
      }
    }

    private void prepareMediaController() {
      mMediaServiceBinder.registerMediaController(mMediaController);
      if (mMediaController != null) {
        mMediaController.setMediaPlayer(mMediaServiceBinder);
        mMediaController.setEnabled(true);
        mMediaController.updatePausePlay();
      }
    }

    @Override
    public void onServiceDisconnected(final ComponentName component) {
      if (component.equals(
              new ComponentName(getActivity(), MediaService.class))) {
        Timber.w("Media service suddenly disconnected");
        if (mMediaController != null) {
          mMediaController.setMediaPlayer(null);
        } else {
          Timber.w(
              "No media controller to release when disconnected from media service");
        }
        mMediaServiceBinder = null;
        mMediaServiceConnection = null;
      }
    }
  }

  /**
   * Opens the previewed file with an external application.
   */
  private void openFile() {
    stopPreview();
    mContainerActivity.getFileOperationsHelper().openFile(getFile());
    finish();
  }

  /**
   * Helper method to test if an {@link OCFile} can be passed to a {@link
   * PreviewAudioFragment} to be previewed.
   *
   * @param file File to test if can be previewed.
   * @return 'True' if the file can be handled by the fragment.
   */
  public static boolean canBePreviewed(final OCFile file) {
    return (file != null && file.isDown() && file.isAudio());
  }

  public void stopPreview() { mMediaServiceBinder.pause(); }

  /**
   * Finishes the preview
   */
  private void finish() { getActivity().onBackPressed(); }
}

/**
 * ownCloud Android client application
 * <p>
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

package com.owncloud.android.services;

import android.accounts.Account;
import android.accounts.AccountsException;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.IndexedForest;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.SingleSessionManager;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.utils.Extras;
import com.owncloud.android.utils.FileStorageUtils;
import java.io.IOException;
import timber.log.Timber;

/**
 * SyncFolder worker. Performs the pending operations in the order they were
 * requested. <p> Created with the Looper of a new thread, started in
 * {@link com.owncloud.android.services.OperationsService#onCreate()}.
 */
class SyncFolderHandler extends Handler {

OperationsService mService;

private IndexedForest<SynchronizeFolderOperation> mPendingOperations =
	new IndexedForest<>();

private OwnCloudClient mOwnCloudClient = null;
private Account mCurrentAccount = null;
private FileDataStorageManager mStorageManager;
private SynchronizeFolderOperation mCurrentSyncOperation;
private LocalBroadcastManager mLocalBroadcastManager;

public SyncFolderHandler(final Looper looper,
                         final OperationsService service) {
	super(looper);
	if (service == null) {
		throw new IllegalArgumentException(
			      "Received invalid NULL in parameter 'service'");
	}
	mService = service;

	// create manager for local broadcasts
	mLocalBroadcastManager = LocalBroadcastManager.getInstance(mService);
}

/**
 * Returns True when the folder located in 'remotePath' in the ownCloud
 * account 'account', or any of its descendants, is being synchronized (or
 * waiting for it).
 *
 * @param account    ownCloud account where the remote folder is stored.
 * @param remotePath The path to a folder that could be in the queue of
 *     synchronizations.
 */
public boolean isSynchronizing(final Account account,
                               final String remotePath) {
	if (account == null || remotePath == null) {
		return false;
	}
	return (mPendingOperations.contains(account.name, remotePath));
}

@Override
public void handleMessage(final Message msg) {
	Pair<Account, String> itemSyncKey = (Pair<Account, String>)msg.obj;
	doOperation(itemSyncKey.first, itemSyncKey.second);
	Timber.d("Stopping after command with id %s", msg.arg1);
	mService.stopSelf(msg.arg1);
}

/**
 * Performs the next operation in the queue
 */
private void doOperation(final Account account, final String remotePath) {

	mCurrentSyncOperation = mPendingOperations.get(account.name, remotePath);

	if (mCurrentSyncOperation != null) {
		RemoteOperationResult result = null;

		try {

			if (mCurrentAccount == null || !mCurrentAccount.equals(account)) {
				mCurrentAccount = account;
				mStorageManager = new FileDataStorageManager(
					mService, account, mService.getContentResolver());
			} // else, reuse storage manager from previous operation

			// always get client from client manager, to get fresh credentials in
			// case of update
			OwnCloudAccount ocAccount = new OwnCloudAccount(account, mService);
			mOwnCloudClient =
				SingleSessionManager.getDefaultSingleton().getClientFor(ocAccount,
				                                                        mService);

			result =
				mCurrentSyncOperation.execute(mOwnCloudClient, mStorageManager);

		} catch (AccountsException | IOException e) {
			Timber.e(e, "Error while trying to get authorization");
		} finally {
			mPendingOperations.removePayload(account.name, remotePath);

			mService.dispatchResultToOperationListeners(mCurrentSyncOperation,
			                                            result);

			sendBroadcastFinishedSyncFolder(account, remotePath,
			                                result != null && result.isSuccess());
		}
	}
}

public void add(final Account account, final String remotePath,
                final SynchronizeFolderOperation syncFolderOperation) {
	Pair<String, String> putResult = mPendingOperations.putIfAbsent(
		account.name, remotePath, syncFolderOperation);
	if (putResult != null) {
		sendBroadcastNewSyncFolder(account, remotePath); // TODO upgrade!
	}
}

/**
 * Cancels a pending or current sync' operation.
 *
 * @param account ownCloud {@link Account} where the remote file is stored.
 * @param file    A file in the queue of pending synchronizations
 */
public void cancel(final Account account, final OCFile file) {
	if (account == null || file == null) {
		Timber.e("Cannot cancel with NULL parameters");
		return;
	}
	Pair<SynchronizeFolderOperation, String> removeResult =
		mPendingOperations.remove(account.name, file.getRemotePath());
	SynchronizeFolderOperation synchronization = removeResult.first;
	if (synchronization != null) {
		synchronization.cancel();
	} else {
		// TODO synchronize?
		if (mCurrentSyncOperation != null && mCurrentAccount != null &&
		    mCurrentSyncOperation.getRemotePath().startsWith(
			    file.getRemotePath()) &&
		    account.name.equals(mCurrentAccount.name)) {
			mCurrentSyncOperation.cancel();
		}
	}
}

/**
 * TODO review this method when "folder synchronization" replaces "folder
 * download"; this is a fast and ugly patch.
 */
private void sendBroadcastNewSyncFolder(final Account account,
                                        final String remotePath) {
	Intent added = new Intent(FileDownloader.getDownloadAddedMessage());
	added.putExtra(Extras.EXTRA_ACCOUNT_NAME, account.name);
	added.putExtra(Extras.EXTRA_REMOTE_PATH, remotePath);
	added.putExtra(Extras.EXTRA_FILE_PATH,
	               FileStorageUtils.getSavePath(account.name) + remotePath);
	mLocalBroadcastManager.sendBroadcast(added);
}

/**
 * TODO review this method when "folder synchronization" replaces "folder
 * download"; this is a fast and ugly patch.
 */
private void sendBroadcastFinishedSyncFolder(final Account account,
                                             final String remotePath,
                                             final boolean success) {
	Intent finished = new Intent(FileDownloader.getDownloadFinishMessage());
	finished.putExtra(Extras.EXTRA_ACCOUNT_NAME, account.name);
	finished.putExtra(Extras.EXTRA_REMOTE_PATH, remotePath);
	finished.putExtra(Extras.EXTRA_FILE_PATH,
	                  FileStorageUtils.getSavePath(account.name) + remotePath);
	finished.putExtra(Extras.EXTRA_DOWNLOAD_RESULT, success);
	mLocalBroadcastManager.sendBroadcast(finished);
}
}

/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.data.user.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.owncloud.android.data.user.datasources.RemoteUserDataSource
import com.owncloud.android.testutil.OC_USER_INFO
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class OCUserRepositoryTest {
    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private val remoteUserDataSource = mockk<RemoteUserDataSource>(relaxed = true)
    private val ocUserRepository: OCUserRepository = OCUserRepository(remoteUserDataSource)

    @Test
    fun getUserInfo() {
        every { remoteUserDataSource.getUserInfo() } returns OC_USER_INFO

        ocUserRepository.getUserInfo()

        verify(exactly = 1) {
            remoteUserDataSource.getUserInfo()
        }
    }

    @Test(expected = Exception::class)
    fun checkPathExistenceExistsNoConnection() {
        every { remoteUserDataSource.getUserInfo() }  throws Exception()

        ocUserRepository.getUserInfo()

        verify(exactly = 1) {
            remoteUserDataSource.getUserInfo()
        }
    }
}

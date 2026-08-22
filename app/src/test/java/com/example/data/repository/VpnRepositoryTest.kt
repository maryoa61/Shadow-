package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ServerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VpnRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: VpnRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VpnRepository(
            database.serverDao(),
            database.logDao(),
            database.appSettingsDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `initialization creates settings when servers already exist`() = runTest {
        database.serverDao().insertServer(testServer(alias = "Restored node"))

        repository.initializeDefaultDataIfEmpty()

        assertEquals(1, repository.allServers.first().size)
        assertNotNull(repository.settings.first())
    }

    @Test
    fun `deleting selected server selects a remaining fallback`() = runTest {
        repository.initializeDefaultDataIfEmpty()
        val selectedBefore = repository.selectedServer.first()
        assertNotNull(selectedBefore)

        repository.deleteServer(selectedBefore!!)

        val selectedAfter = repository.selectedServer.first()
        assertNotNull(selectedAfter)
        assertNotEquals(selectedBefore.id, selectedAfter!!.id)
        assertEquals(3, repository.allServers.first().size)
    }

    @Test
    fun `inserting into empty database selects the first server`() = runTest {
        val insertedId = repository.insertServer(testServer(alias = "First node"))

        val selected = repository.selectedServer.first()
        assertNotNull(selected)
        assertEquals(insertedId, selected!!.id)
        assertTrue(selected.isSelected)
    }

    private fun testServer(alias: String) = ServerEntity(
        alias = alias,
        address = "server.example.com",
        port = 443,
        uuid = "12345678-abcd-ef01-2345-6789abcdef01"
    )
}

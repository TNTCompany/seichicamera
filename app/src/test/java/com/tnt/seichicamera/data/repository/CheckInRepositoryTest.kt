package com.tnt.seichicamera.data.repository

import com.tnt.seichicamera.data.local.dao.CheckInDao
import com.tnt.seichicamera.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckInRepositoryTest {

    private lateinit var fakeCheckInDao: FakeCheckInDao
    private lateinit var repository: CheckInRepository

    @Before
    fun setUp() {
        fakeCheckInDao = FakeCheckInDao()
        repository = CheckInRepository(fakeCheckInDao)
    }

    @Test
    fun `checkIn inserts record into dao and returns id`() = runTest {
        val id = repository.checkIn(
            pointId = "pt_100",
            photoUri = "content://media/1",
            comparisonUri = "content://media/comp_1"
        )

        assertEquals(1L, id)
        assertEquals(1, fakeCheckInDao.items.size)
        val saved = fakeCheckInDao.items[0]
        assertEquals("pt_100", saved.pointId)
        assertEquals("content://media/1", saved.photoUri)
        assertEquals("content://media/comp_1", saved.comparisonUri)
    }

    @Test
    fun `getCheckedInPointIds returns flow of IDs`() = runTest {
        fakeCheckInDao.pointIdsFlow.value = listOf("pt_1", "pt_2", "pt_3")

        val ids = repository.getCheckedInPointIds().first()

        assertEquals(listOf("pt_1", "pt_2", "pt_3"), ids)
    }

    @Test
    fun `isCheckedIn returns true when record exists and false otherwise`() = runTest {
        assertFalse(repository.isCheckedIn("pt_999"))

        repository.checkIn("pt_999", "content://media/999")

        assertTrue(repository.isCheckedIn("pt_999"))
    }

    // --- Fake DAO ---

    private class FakeCheckInDao : CheckInDao {
        val items = mutableListOf<CheckInEntity>()
        val pointIdsFlow = MutableStateFlow<List<String>>(emptyList())
        private var nextId = 1L

        override suspend fun insert(checkIn: CheckInEntity): Long {
            val assignedId = nextId++
            val item = checkIn.copy(id = assignedId)
            items.add(item)
            pointIdsFlow.value = items.map { it.pointId }.distinct()
            return assignedId
        }

        override suspend fun getByPointId(pointId: String): CheckInEntity? =
            items.find { it.pointId == pointId }

        override fun getAllCheckedInPointIds(): Flow<List<String>> = pointIdsFlow

        override fun getAllCheckIns(): Flow<List<CheckInEntity>> =
            MutableStateFlow(items)
    }
}

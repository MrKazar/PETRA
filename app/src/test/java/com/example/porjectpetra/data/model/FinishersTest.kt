package com.example.porjectpetra.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FinishersTest {

    private val single = Finisher("solo", "Solo", FinisherPool.EPAULES_BRAS, listOf("10 pompes piquées"))
    private val a = Finisher("a", "A", FinisherPool.ABDOS, listOf("30 abdos"))
    private val b = Finisher("b", "B", FinisherPool.ABDOS, listOf("15 V-ups"))
    private val c = Finisher("c", "C", FinisherPool.ABDOS, listOf("45 s de gainage"))
    private val catalog = listOf(single, a, b, c)

    @Test
    fun neverRepeatsLastIdWhenPoolHasSeveral() {
        val random = Random(42)
        repeat(200) {
            val picked = pickFinisher(FinisherPool.ABDOS, lastId = "b", random = random, catalog = catalog)
            assertNotNull(picked)
            assertNotEquals("b", picked!!.id)
            assertEquals(FinisherPool.ABDOS, picked.pool)
        }
    }

    @Test
    fun drawsAllOtherFinishersOfPool() {
        val random = Random(7)
        val ids = (1..200).mapNotNull { pickFinisher(FinisherPool.ABDOS, "a", random, catalog)?.id }.toSet()
        assertEquals(setOf("b", "c"), ids)
    }

    @Test
    fun singleItemPoolAlwaysReturnsIt_evenIfLast() {
        assertEquals(single, pickFinisher(FinisherPool.EPAULES_BRAS, lastId = "solo", random = Random(1), catalog = catalog))
        assertEquals(single, pickFinisher(FinisherPool.EPAULES_BRAS, lastId = null, random = Random(1), catalog = catalog))
    }

    @Test
    fun emptyPoolReturnsNull() {
        assertNull(pickFinisher(FinisherPool.DOS, lastId = null, random = Random(1), catalog = catalog))
    }

    @Test
    fun builtInCatalog_availablePoolsHaveAFinisher_andIdsAreUnique() {
        assertTrue(availableFinisherPools.containsAll(VALIDATED_FINISHERS.map { it.pool }))
        availableFinisherPools.forEach { pool ->
            assertNotNull(pool.name, pickFinisher(pool, lastId = null, random = Random(3)))
        }
        assertEquals(FINISHERS.size, FINISHERS.map { it.id }.toSet().size)
    }
}

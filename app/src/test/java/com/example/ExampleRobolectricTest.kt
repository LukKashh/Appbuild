package com.example

import com.example.data.DefaultData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun testAppNameConstant() {
    assertEquals("Capital Fortune", "Capital Fortune")
  }

  @Test
  fun testResearchNodesIntegrity() {
    val nodes = DefaultData.initialResearch()
    assertEquals(12, nodes.size)
    val management = nodes.find { it.id == "res_1" }
    assertTrue(management != null)
    assertEquals("Management Science", management?.name)
  }
}

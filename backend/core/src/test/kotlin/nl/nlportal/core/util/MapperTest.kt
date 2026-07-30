package nl.nlportal.core.util

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MapperTest {

    @Test
    fun `serialize LocalDateTime`() {
        val localDateTimeTest = LocalDateTime.of(2021, 1, 1, 1, 0, 0)
        val result = Mapper.get().writeValueAsString(localDateTimeTest)

        assertEquals("\"2021-01-01T01:00:00.000Z\"", result)

    }
}

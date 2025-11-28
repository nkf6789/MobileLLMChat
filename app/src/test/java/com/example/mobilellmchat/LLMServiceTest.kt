// 新建文件：app/src/test/java/com/example/mobilellmchat/LLMServiceTest.kt
package com.example.mobilellmchat

import com.example.mobilellmchat.model.*
import org.junit.Test
import org.junit.Assert.*

class LLMServiceTest {

    @Test
    fun testInterfaceDefinition() {
        val mockService = object : LLMService {
            override suspend fun chat(messages: List<ApiMessage>): String {
                return "Mock Reply"
            }

            override fun getModelInfo(): ModelInfo {
                return ModelInfo(
                    name = "Mock Model",
                    type = ModelType.REMOTE
                )
            }
        }

        assertEquals("Mock Model", mockService.getModelInfo().name)
        assertEquals(ModelType.REMOTE, mockService.getModelInfo().type)
    }
}

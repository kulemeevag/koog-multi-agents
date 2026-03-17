package ru.kulemeev.app.config

import kotlinx.serialization.json.Json
import java.io.File

class JsonFileConfigLoader(private val fileName: String = "config.json") : ConfigLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    override fun loadConfig(): ChatConfig {
        val file = File(fileName)
        
        // 1. Try external file
        if (file.exists()) {
            return try {
                val content = file.readText()
                json.decodeFromString(ChatConfig.serializer(), content)
            } catch (e: Exception) {
                System.err.println("Error reading config file $fileName: ${e.message}. Using defaults.")
                ChatConfig()
            }
        }

        // 2. Try resource from classpath
        val resourceStream = this::class.java.getResourceAsStream("/$fileName")
        val config = if (resourceStream != null) {
            try {
                val content = resourceStream.bufferedReader().use { it.readText() }
                json.decodeFromString(ChatConfig.serializer(), content)
            } catch (e: Exception) {
                System.err.println("Error reading config from resources: ${e.message}")
                ChatConfig()
            }
        } else {
            ChatConfig()
        }

        // 3. Persist to external file so user can edit it
        try {
            val content = json.encodeToString(ChatConfig.serializer(), config)
            file.writeText(content)
            println("Created local config file from defaults: $fileName")
        } catch (e: Exception) {
            System.err.println("Could not create local config file: ${e.message}")
        }

        return config
    }
}

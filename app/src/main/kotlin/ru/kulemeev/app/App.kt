package ru.kulemeev.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.runBlocking

class ChatAppCommand : CliktCommand(name = "koog-chat") {
    private val logLevel by option(help = "Ktor log level (NONE, INFO, HEADERS, BODY)")
        .enum<LogLevel> { it.name }
        .default(LogLevel.NONE)

    private val apiKey by option(envvar = "OPENROUTER_API_KEY", help = "OpenRouter API Key")

    override fun run() {
        val key = apiKey ?: run {
            System.err.println("Error: OPENROUTER_API_KEY is not set. Use --api-key or set the environment variable.")
            return
        }

        runBlocking {
            ChatApplication().run(key, logLevel)
        }
    }
}

fun main(args: Array<String>) = ChatAppCommand().main(args)

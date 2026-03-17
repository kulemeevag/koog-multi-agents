package ru.kulemeev.app.config

interface ConfigLoader {
    fun loadConfig(): ChatConfig
}

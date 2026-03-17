# Koog Multi-Agents Chatbot

This project is a multi-agent chatbot system that interacts with LLMs through a colorful terminal interface.

## Features
- **Interactive Console UI**: User-friendly chat interface with support for multiple turns.
- **ANSI Color Support**: Distinguished message types for better readability:
    - **System Messages**: Green (Welcome/Goodbye)
    - **LLM Responses**: Yellow
    - **Reasoning/Temperature**: Cyan
    - **Tool Calls**: Magenta
- **LLM Integration**: Integrated with an LLM service (Gemini/Vertex AI).

## Project Structure
This project follows a standard Kotlin/Gradle multi-module setup:
- `app`: The main module containing the chatbot logic, UI, and LLM services.
- `buildSrc`: Contains shared build logic and convention plugins.

## How to Build and Run
This project uses [Gradle](https://gradle.org/). To build and run the application, use the Gradle wrapper:

* Run `./gradlew :app:run` to build and start the chat session.
* Run `./gradlew build` to compile the project and run all tests.
* Run `./gradlew clean` to remove build artifacts.

Note the usage of the Gradle Wrapper (`./gradlew`). This is the recommended way to interact with Gradle.

### Usage Tips
- Upon starting, the app will ask for a **Temperature** setting (default is 0.7).
- Type your message and press **Enter** to chat.
- Type `exit` or `quit` to end the session.

---
[Learn more about the Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html).

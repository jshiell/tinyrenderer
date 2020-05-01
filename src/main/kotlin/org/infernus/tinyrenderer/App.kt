package org.infernus.tinyrenderer

class App {
    val greeting: String
        get() {
            return "Hello world."
        }
}

fun main() {
    println(App().greeting)
}

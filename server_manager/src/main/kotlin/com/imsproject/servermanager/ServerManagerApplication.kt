package com.imsproject.servermanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class DockerControllerApplication

fun main(args: Array<String>) {
    runApplication<DockerControllerApplication>(*args)
}

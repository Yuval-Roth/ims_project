package com.imsproject.servermanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DockerControllerApplication

fun main(args: Array<String>) {
    runApplication<DockerControllerApplication>(*args)
}

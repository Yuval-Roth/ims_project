package com.imsproject.docker_controller

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping

@SpringBootApplication
class DockerControllerApplication

fun main(args: Array<String>) {
    runApplication<DockerControllerApplication>(*args)
}

@GetMapping("/")
fun home(): String {
    return "404"
}

@GetMapping("/docker/update")
fun updateDocker(): String {
    ProcessBuilder("sh", "/home/admin/pull_and_restart").start()
    return "great success"
}

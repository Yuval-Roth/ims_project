package com.imsproject.docker_controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class App {
    @GetMapping("/")
    fun home(): String {
        return "404"
    }

    @GetMapping("/docker/update")
    fun updateDocker(): String {
        ProcessBuilder("sh", "/home/admin/pull_and_restart").start()
        return "great success"
    }
}
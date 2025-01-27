package com.imsproject.gameserver.dataAccess

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource


@Configuration
class PostgresConfig {
    @Value("\${running.local}")
    private var runningLocal: Boolean = false

    @Value("\${database.scheme}")
    private var scheme: String = ""
    @Value("\${database.port}")
    private var port: Int = 0
    @Value("\${database.name}")
    private var name: String = ""
    @Value("\${database.host}")
    private var host: String = ""
    @Value("\${database.driver-class-name}")
    private var driverClassName: String = ""

    private val username: String = System.getenv("POSTGRES_USER")
    private val password: String = System.getenv("POSTGRES_PASSWORD")

    @Bean
    fun dataSource(): DataSource {

        if(runningLocal){
            host = "localhost"
        }

        val url = "$scheme://$host:$port/$name"
        return DataSourceBuilder.create()
            .url(url)
            .username(username)
            .password(password)
            .driverClassName(driverClassName)
            .type(HikariDataSource::class.java)
            .build()
    }

    @Bean
    fun sqlExecutor(): PostgreSQLExecutor {

        if(runningLocal){
            host = "localhost"
        }

        val url = "$scheme://$host:$port/$name"
        return PostgreSQLExecutor(url,username,password)
    }
}

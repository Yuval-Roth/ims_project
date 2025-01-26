package com.imsproject.gameserver.dataAccess

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

    // ================================ |
    // TODO: REPLACE WITH ENVIRONMENT VARIABLES
    @Value("\${database.username}")
    private var username: String = ""
    @Value("\${database.password}")
    private var password: String = ""
    // ================================ |

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
            .build()
    }
}

package com.wire.bots.echo

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.wire.bots.echo.Service.Config
import com.wire.bots.sdk.Configuration
import com.wire.bots.sdk.Server
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment

class Service : Server<Config>() {
    override fun initialize(bootstrap: Bootstrap<Config>) {
        super.initialize(bootstrap)
        bootstrap.objectMapper.registerKotlinModule()
    }

    override fun initialize(config: Config, env: Environment) {
        env.jersey().urlPattern = "/echo-kt/*"
    }

    override fun createHandler(config: Config, env: Environment) = MessageHandler(this)

    data class Config(val ingress: String, val portMin: Int, val portMax: Int, val module: String) : Configuration()
}

fun main(args: Array<String>) {
    Service().run(*args)
}

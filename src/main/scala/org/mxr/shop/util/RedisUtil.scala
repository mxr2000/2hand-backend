package org.mxr.shop.util

import dev.profunktor.redis4cats.Redis
import cats.effect.IO
import dev.profunktor.redis4cats.effect.Log.Stdout.*

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object RedisUtil {
  private val uri = "redis://pdnfXxD3nMqlkY0uZTpo9yx4qHmWusMR@redis-11175.c245.us-east-1-3.ec2.cloud.redislabs.com:11175"

  def setAuthCode(email: String, code: String): IO[Unit] = {
    Redis[IO].utf8(uri).use { redis =>
      redis.set(s"auth:$email", code) >>
        redis.expire(s"auth:$email", FiniteDuration(10L, TimeUnit.MINUTES)) >>
        IO.unit
    }
  }

  def getAuthCode(email: String): IO[Option[String]] = {
    Redis[IO].utf8(uri).use { redis =>
      redis.get(s"auth:$email")
    }
  }

}

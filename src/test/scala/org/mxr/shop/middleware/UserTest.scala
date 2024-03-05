package org.mxr.shop.middleware

import org.mxr.shop.exception.Exception.LogInError
import org.mxr.shop.service.UserService

class UserTest extends munit.FunSuite {
  import cats.effect.unsafe.implicits.global
  val userService: UserService = UserService.userService
  test("log in with wrong email") {
    userService.logIn("", "").unsafeRunSync() match {
      case Left(value: LogInError.EmailDoesNotExist) =>
      case _                                         => fail("")
    }
  }

  test("log in with wrong password") {}

  test("register with already existent email") {}

  test("add contact") {}

  test("delete contact") {}

  test("update information") {
    userService.updateInformation("", "", "", "")
  }

}

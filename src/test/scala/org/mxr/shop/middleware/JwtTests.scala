package org.mxr.shop.middleware

import org.mxr.shop.util.JwtUtil

class JwtTests extends munit.FunSuite {
  test("jwt") {
    val str = JwtUtil.encodeJwt("""{"a": "a"}""")
    println(JwtUtil.validateJwt(str))
  }
}

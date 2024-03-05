package org.mxr.shop.middleware

import org.mxr.shop.util.EmailUtil

class EmailTest extends munit.FunSuite {
  test("send") {
    EmailUtil.sendEmail("1099493928@qq.com",
      "syyvipxyiqcdhcaa",
      //"gnh4ku@virginia.edu",
      "1099493928@qq.com",
      "Auth code",
      "123456")
  }
}

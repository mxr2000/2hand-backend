package org.mxr.shop.middleware

import org.mxr.shop.service.{ItemBatchService, UserService, ItemService}

class BatchTest extends munit.FunSuite {
  test("create batch with wrong user") {
    val a = 2
    val b = 2
    assertEquals(a, b)
  }

  test("create batch successfully") {

  }

  // all items in this batch should be inactivate
  test("deactivate batch") {

  }

  // item should bw removed from batch, with status unchanged
  test("remove item from batch") {

  }


}

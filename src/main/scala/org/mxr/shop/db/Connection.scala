package org.mxr.shop.db

import slick.jdbc.MySQLProfile.api.*

object Connection {
  val db = Database.forConfig("postgresDev")
}

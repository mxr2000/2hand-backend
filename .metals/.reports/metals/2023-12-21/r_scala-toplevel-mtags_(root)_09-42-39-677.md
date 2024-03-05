error id: file://<WORKSPACE>/src/main/scala/model/Item.scala:[684..692) in Input.VirtualFile("file://<WORKSPACE>/src/main/scala/model/Item.scala", "package model

case class Item(
    id: Int,
    user: String,
    title: String,
    description: String,
    createTime: Int,
    status: Int,
    price: Float
               )

object ItemTable {
  import slick.jdbc.MySQLProfile.api._
  import slick.lifted.{ProvenShape, Tag}

  class ItemTable(tag: Tag) extends Table[Item](tag, Some("item"), "Item") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def user = column[String]("user")
    def title = column[String]("title")
    def description = column[String]("description")
    def createTime = column[Int]("create_time")
    def status = column[Int]("status")
    def price = column[Float]("price")

    def 

    override def * =
      (id, user, title, description, createTime, status, price) <> (Item.tupled, Item.unapply)
  }

  lazy val itemTable = TableQuery[ItemTable]
}
")
file://<WORKSPACE>/src/main/scala/model/Item.scala
file://<WORKSPACE>/src/main/scala/model/Item.scala:28: error: expected identifier; obtained override
    override def * =
    ^
#### Short summary: 

expected identifier; obtained override
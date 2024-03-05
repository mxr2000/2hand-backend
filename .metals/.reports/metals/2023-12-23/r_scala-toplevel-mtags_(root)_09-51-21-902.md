error id: file://<WORKSPACE>/src/main/scala/service/UserService.scala:[165..165) in Input.VirtualFile("file://<WORKSPACE>/src/main/scala/service/UserService.scala", "package service

import cats.effect.IO
import model.User

trait UserService {
  def logIn(email: String, passwordHash: String): IO[Either[String, User]]

}

object 
")
file://<WORKSPACE>/src/main/scala/service/UserService.scala
file://<WORKSPACE>/src/main/scala/service/UserService.scala:12: error: expected identifier; obtained eof

^
#### Short summary: 

expected identifier; obtained eof
package org.mxr.shop.exception

import scala.util.control.NoStackTrace

object Exception {

  sealed trait AuthorizationError extends NoStackTrace

  case object WrongUser extends AuthorizationError


  enum SignUpError(msg: String) extends RuntimeException(msg) {
    case EmailAlreadyExists(email: String)
      extends SignUpError(s"Email $email already exists")
    case AuthCodeDoesNotExist(authCode: String)
      extends SignUpError(s"Auth code $authCode does not exist in Redis")
    case AuthCodeIncorrect(authCode: String)
      extends SignUpError(s"Auth code $authCode does not match that in Redis")
    case UsernameAlreadyExists(username: String)
      extends SignUpError(s"Username $username already exists")
    case UnknownError extends SignUpError("Unknown error")
  }

  enum LogInError(msg: String) extends RuntimeException(msg) {
    case EmailDoesNotExist(email: String)
      extends LogInError(s"Email $email does not exist")
    case PasswordError(email: String)
      extends LogInError(s"Password for $email is wrong")
  }

  enum RequestAuthenticationError(msg: String) extends RuntimeException(msg) {
    case EmailAlreadyExists(email: String)
      extends RequestAuthenticationError(s"Email $email already exists")
    case SendEmailError(error: String) extends RequestAuthenticationError(s"")
    case WrongEmail(email: String) extends RequestAuthenticationError(s"This email cannot register: $email")
    case UnknownError(throwable: Throwable) extends RequestAuthenticationError(throwable.getMessage)
  }


}

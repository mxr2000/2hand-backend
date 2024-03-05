package org.mxr.shop.util

import cats.effect.IO
import com.sun.mail.util.MailSSLSocketFactory
import org.mxr.shop.exception.Exception.RequestAuthenticationError

import javax.mail.*
import javax.mail.internet.*
import java.util.Properties

object EmailUtil {
  def sendEmail(
      from: String,
      password: String,
      to: String,
      subject: String,
      body: String
  ): IO[Unit] = {
    val properties = new Properties()
    properties.put("mail.transport.protocol", "smtp")
    properties.put("mail.smtp.auth", "true")
    properties.put("mail.smtp.ssl.enable", "true")
    properties.put("mail.user", from)
    properties.put("mail.password", password)
    properties.put("mail.smtp.host", "smtp.qq.com")
    properties.put("mail.smtp.port", "465")
    properties.put("mail.smtp.timeout", 2000)

    val msf = MailSSLSocketFactory();
    msf.setTrustAllHosts(true);
    properties.put("mail.smtp.ssl.socketFactory", msf)

    val session = Session.getInstance(
      properties,
      new Authenticator() {
        override protected def getPasswordAuthentication
            : PasswordAuthentication = {
          new PasswordAuthentication(from, password)
        }
      }
    )
    session.setDebug(true);

    val message = new MimeMessage(session)
    message.setFrom(new InternetAddress(from))
    message.addRecipient(Message.RecipientType.TO, new InternetAddress(to))
    message.setSubject(subject, "UTF-8")
    message.setText(body, "UTF-8")
    IO.blocking({
      Transport.send(message)
    }).handleErrorWith {
      case e: MessagingException => IO.raiseError(RequestAuthenticationError.SendEmailError(e.getMessage))
    }
  }
}

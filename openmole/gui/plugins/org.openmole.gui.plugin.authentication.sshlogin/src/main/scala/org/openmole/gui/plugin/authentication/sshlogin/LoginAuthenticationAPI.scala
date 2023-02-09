package org.openmole.gui.plugin.authentication.sshlogin

import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.api.*
import org.openmole.core.services.Services
import scala.concurrent.Future

trait LoginAuthenticationAPI:
  def loginAuthentications(): Future[Seq[LoginAuthenticationData]]
  def addAuthentication(data: LoginAuthenticationData): Future[Unit]
  def removeAuthentication(data: LoginAuthenticationData): Future[Unit]
  def testAuthentication(data: LoginAuthenticationData): Future[Seq[Test]]

object LoginAuthenticationServerAPI:
  class APIClientImpl(val settings: ClientSettings) extends LoginAuthenticationRESTAPI with APIClient
  def PluginFetch = OMFetch(new APIClientImpl(_))

class LoginAuthenticationServerAPI extends LoginAuthenticationAPI:
  import LoginAuthenticationServerAPI.PluginFetch
  override def loginAuthentications(): Future[Seq[LoginAuthenticationData]] = PluginFetch.future(_.loginAuthentications(()).future)
  override def addAuthentication(data: LoginAuthenticationData): Future[Unit] = PluginFetch.future(_.addAuthentication(data).future)
  override def removeAuthentication(data: LoginAuthenticationData): Future[Unit] = PluginFetch.future(_.removeAuthentication(data).future)
  override def testAuthentication(data: LoginAuthenticationData): Future[Seq[Test]] = PluginFetch.future(_.testAuthentication(data).future)

class LoginAuthenticationStubAPI extends LoginAuthenticationAPI:
  import LoginAuthenticationServerAPI.PluginFetch
  override def loginAuthentications(): Future[Seq[LoginAuthenticationData]] = Future.successful {
    Seq(
      LoginAuthenticationData(
        "stub",
        "stub",
        "stub.stub.stub"
      ),
      LoginAuthenticationData(
        "stub2",
        "stub2",
        "stub2.stub.stub"
      )
    )
  }

  override def addAuthentication(data: LoginAuthenticationData): Future[Unit] = Future.successful(())
  override def removeAuthentication(data: LoginAuthenticationData): Future[Unit] = Future.successful(())
  override def testAuthentication(data: LoginAuthenticationData): Future[Seq[Test]] = Future.successful(Seq())



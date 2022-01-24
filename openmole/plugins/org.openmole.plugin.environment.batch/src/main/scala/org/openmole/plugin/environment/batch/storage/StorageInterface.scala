/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.plugin.environment.batch.storage

import java.io.{ ByteArrayInputStream, File, InputStream }
import java.nio.file.Files

import gridscale._
import org.openmole.core.communication.storage._
import org.openmole.core.workspace._
import org.openmole.plugin.environment.batch.environment.{ BatchEnvironment, AccessControl }
import org.openmole.tool.file._
import org.openmole.tool.stream._

object StorageInterface {

  def remote[S: StorageInterface: HierarchicalStorageInterface](s: S, communicationDirectory: String) =
    new RemoteStorage {
      override def upload(src: File, dest: Option[String], options: TransferOptions)(implicit newFile: TmpDirectory): String = StorageService.uploadInDirectory(s, src, communicationDirectory, options)
      override def download(src: String, dest: File, options: TransferOptions)(implicit newFile: TmpDirectory): Unit = StorageService.download(s, src, dest, options)
    }

  def upload(compressed: Boolean, uploadStream: (() ⇒ InputStream, String) ⇒ Unit)(src: File, dest: String, options: TransferOptions = TransferOptions.default): Unit = {
    def fileStream() = src.bufferedInputStream()

    if (compressed) {
      def compressedFileStream() = src.bufferedInputStream().toGZiped
      if (!options.raw) uploadStream(compressedFileStream, dest) else uploadStream(fileStream, dest)
    }
    else uploadStream(fileStream, dest)
  }

  def download(compressed: Boolean, downloadStream: (String, InputStream ⇒ Unit) ⇒ Unit)(src: String, dest: File, options: TransferOptions = TransferOptions.default): Unit = {
    def downloadFile(is: InputStream) = Files.copy(is, dest.toPath)
    if (compressed) {
      def uncompressed(is: InputStream) = downloadFile(is.toGZ)
      if (!options.raw) downloadStream(src, uncompressed) else downloadStream(src, downloadFile)
    }
    else downloadStream(src, downloadFile)
  }

  def isDirectory(name: String) = name.endsWith("/")

}

trait StorageInterface[T] {
  def exists(t: T, path: String): Boolean
  def rmFile(t: T, path: String): Unit
  def upload(t: T, src: File, dest: String, options: TransferOptions = TransferOptions.default): Unit
  def download(t: T, src: String, dest: File, options: TransferOptions = TransferOptions.default): Unit
}

trait HierarchicalStorageInterface[T] {
  def rmDir(t: T, path: String): Unit
  def makeDir(t: T, path: String): Unit
  def child(t: T, parent: String, child: String): String
  def list(t: T, path: String): Seq[ListEntry]
  def parent(t: T, path: String): Option[String]
  def name(t: T, path: String): String
}

trait EnvironmentStorage[S] {
  def id(s: S): String
  def environment(s: S): BatchEnvironment
}
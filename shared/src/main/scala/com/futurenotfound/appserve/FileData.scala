package com.futurenotfound.appserve

import java.io.File
import java.security.MessageDigest
import java.util.jar.{JarFile, JarEntry}
import org.apache.commons.io.{IOUtils, FileUtils}

case class FileData(name: String, checksum: String)

object FileData {
  val digest = MessageDigest.getInstance("MD5")
  def apply(file: File) = new FileData(file.getName(), new String(digest.digest(FileUtils.readFileToByteArray(file))))
  def apply(jarFile: JarFile, jarEntry: JarEntry) = new FileData(jarEntry.getName(), new String(digest.digest(readJarFileBytes(jarFile, jarEntry))))

  private[this] def readJarFileBytes(jarFile: JarFile, jarEntry: JarEntry): Array[Byte] = {
    val inputStream = jarFile.getInputStream(jarEntry)
    try {
      return digest.digest(IOUtils.toByteArray(inputStream))
    } finally {
      IOUtils.closeQuietly(inputStream)
    }
  }

  def calculateForDirectory(directory: File) = IO.filesFromDir(directory)
                                                 .map(file => FileData(file))
}
package com.futurenotfound.appserve

import java.io.File
import scala.collection.JavaConversions._
import java.util.jar.{JarEntry, JarFile}

object IO {
  val fileFilter = (file: File) => file.isFile
  val directoryFilter = (file: File) => file.isDirectory
  val directorySubElements = (dir: File) => dir.ensuring(dir.isDirectory, "File instance passed is not a directory.")
                                               .listFiles()
  def filesFromDir(dir: File) = directorySubElements(dir).filter(fileFilter)
  def directoriesFromDir(dir: File) = directorySubElements(dir).filter(directoryFilter)

  def classFilesFromJar(jarFile: JarFile): Iterator[JarEntry] = enumerationAsScalaIterator(jarFile.entries())
                                                .filter(entry => entry.getName.endsWith(".class"))
  def classFilesFromJar(file: File): Iterator[JarEntry] = classFilesFromJar(new JarFile(file.ensuring(file.isFile, "File instance passed is not a file.")))
}
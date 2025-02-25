/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.common.util

import java.io.File
import java.net.{URL, URLClassLoader}
import java.util.function.Supplier

object ClassLoaderUtils extends Logger {

  private[this] val originalClassLoader: ClassLoader = Thread.currentThread().getContextClassLoader

  /**
   * 指定 classLoader执行代码...
   *
   * @param targetClassLoader
   * @param func
   * @tparam R
   * @return
   */
  def runAsClassLoader[R](targetClassLoader: ClassLoader, func: () => R): R = {
    try {
      Thread.currentThread.setContextClassLoader(targetClassLoader)
      func()
    } catch {
      case e: Exception => throw e
    } finally {
      Thread.currentThread.setContextClassLoader(originalClassLoader)
    }
  }

  /**
   * 指定 classLoader执行代码...
   * for java
   *
   * @param targetClassLoader
   * @param supplier
   * @tparam R
   * @return
   */
  def runAsClassLoader[R](targetClassLoader: ClassLoader, supplier: Supplier[R]): R = {
    try {
      Thread.currentThread.setContextClassLoader(targetClassLoader)
      supplier.get()
    } catch {
      case e: Exception => throw e
    } finally {
      Thread.currentThread.setContextClassLoader(originalClassLoader)
    }
  }

  def loadJar(jarFilePath: String): Unit = {
    val jarFile = new File(jarFilePath)
    require(jarFile.exists, s"[StreamX] ClassLoaderUtils.loadJar: jarFilePath $jarFilePath is not exists")
    require(jarFile.isFile, s"[StreamX] ClassLoaderUtils.loadJar: jarFilePath $jarFilePath is not file")
    loadPath(jarFile.getAbsolutePath, List(".jar", ".zip"))
  }

  def loadJars(path: String): Unit = {
    val jarDir = new File(path)
    require(jarDir.exists, s"[StreamX] ClassLoaderUtils.loadJars: jarPath $path is not exists")
    require(jarDir.isDirectory, s"[StreamX] ClassLoaderUtils.loadJars: jarPath $path is not directory")
    require(jarDir.listFiles.length > 0, s"[StreamX] ClassLoaderUtils.loadJars: have not jar in path:$path")
    jarDir.listFiles.foreach { x =>
      loadPath(x.getAbsolutePath, List(".jar", ".zip"))
    }
  }


  def loadResource(filepath: String): Unit = {
    val file = new File(filepath)
    addURL(file)
  }

  def loadResourceDir(filepath: String): Unit = {
    val file = new File(filepath)
    loopDirs(file)
  }

  private[this] def loadPath(filepath: String, ext: List[String]): Unit = {
    val file = new File(filepath)
    loopFiles(file, ext)
  }


  private[this] def loopDirs(file: File): Unit = { // 资源文件只加载路径
    if (file.isDirectory) {
      addURL(file)
      file.listFiles.foreach(loopDirs)
    }
  }


  private[this] def loopFiles(file: File, ext: List[String] = List()): Unit = {
    if (file.isDirectory) {
      file.listFiles.foreach(x => loopFiles(x, ext))
    } else {
      if (ext.isEmpty) {
        addURL(file)
      } else if (ext.exists(x => file.getName.endsWith(x))) {
        Utils.checkJarFile(file.toURI.toURL)
        addURL(file)
      }
    }
  }

  private[this] def addURL(file: File): Unit = {
    try {
      val classLoader = ClassLoader.getSystemClassLoader
      classLoader match {
        case c if c.isInstanceOf[URLClassLoader] =>
          val addURL = classOf[URLClassLoader].getDeclaredMethod("addURL", Array(classOf[URL]): _*)
          addURL.setAccessible(true)
          addURL.invoke(c, file.toURI.toURL)
        case _ =>
          val field = classLoader.getClass.getDeclaredField("ucp")
          field.setAccessible(true)
          val ucp = field.get(classLoader)
          val addURL = ucp.getClass.getDeclaredMethod("addURL", Array(classOf[URL]): _*)
          addURL.setAccessible(true)
          addURL.invoke(ucp, file.toURI.toURL)
      }
    } catch {
      case e: Exception => throw e
    }
  }


}

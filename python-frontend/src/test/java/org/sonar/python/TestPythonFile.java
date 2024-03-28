/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.python;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.sonar.plugins.python.api.PythonFile;
import org.sonar.plugins.python.api.tree.FileInput;
import org.sonar.python.parser.PythonParser;
import org.sonar.python.tree.PythonTreeMaker;

public class TestPythonFile implements PythonFile {

  private final Path rootDirectory;
  private final Path filePath;

  public TestPythonFile(Path rootDirectory, Path filePath) {
    this.rootDirectory = rootDirectory;
    this.filePath = filePath;
  }

  @Override
  public String content() {
    try {
      return new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read " + filePath, e);
    }
  }

  @Override
  public String fileName() {
    return filePath.getFileName().toString();
  }

  @Override
  public URI uri() {
    return filePath.toUri();
  }

  @Override
  public String key() {
    return rootDirectory.relativize(filePath).toString();
  }

  public boolean isIPython() {
    return fileName().endsWith(".ipynb");
  }

  public FileInput parseFile() {
    var parser = PythonParser.create();
    var treeMaker = new PythonTreeMaker();

    var astNode = parser.parse(content());
    return treeMaker.fileInput(astNode);
  }

}
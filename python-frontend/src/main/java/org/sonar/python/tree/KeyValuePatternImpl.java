/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.python.tree;

import java.util.List;
import java.util.stream.Stream;
import org.sonar.plugins.python.api.tree.KeyValuePattern;
import org.sonar.plugins.python.api.tree.Pattern;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.TreeVisitor;

public class KeyValuePatternImpl extends PyTree implements KeyValuePattern {

  private final Pattern key;
  private final Token colon;
  private final Pattern value;

  public KeyValuePatternImpl(Pattern key, Token colon, Pattern value) {
    this.key = key;
    this.colon = colon;
    this.value = value;
  }


  @Override
  public Pattern key() {
    return key;
  }

  @Override
  public Token colon() {
    return colon;
  }

  @Override
  public Pattern value() {
    return value;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitKeyValuePattern(this);
  }

  @Override
  public Kind getKind() {
    return Kind.KEY_VALUE_PATTERN;
  }

  @Override
  List<Tree> computeChildren() {
    return Stream.of(key, colon, value).toList();
  }
}

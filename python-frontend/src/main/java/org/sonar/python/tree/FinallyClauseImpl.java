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
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.plugins.python.api.tree.FinallyClause;
import org.sonar.plugins.python.api.tree.StatementList;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.TreeVisitor;

public class FinallyClauseImpl extends PyTree implements FinallyClause {
  private final Token finallyKeyword;
  private final Token colon;
  private final Token newLine;
  private final Token indent;
  private final StatementList body;
  private final Token dedent;

  public FinallyClauseImpl(Token finallyKeyword, Token colon, @Nullable Token newLine, @Nullable Token indent, StatementList body, @Nullable Token dedent) {
    this.finallyKeyword = finallyKeyword;
    this.colon = colon;
    this.newLine = newLine;
    this.indent = indent;
    this.body = body;
    this.dedent = dedent;
  }

  @Override
  public Token finallyKeyword() {
    return finallyKeyword;
  }

  @Override
  public StatementList body() {
    return body;
  }

  @Override
  public Kind getKind() {
    return Kind.FINALLY_CLAUSE;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitFinallyClause(this);
  }

  @Override
  public List<Tree> computeChildren() {
    return Stream.of(finallyKeyword, colon, newLine, indent, body, dedent).filter(Objects::nonNull).toList();
  }
}

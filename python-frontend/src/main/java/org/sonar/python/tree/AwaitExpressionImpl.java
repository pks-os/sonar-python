/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2025 SonarSource SA
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
import org.sonar.plugins.python.api.tree.AwaitExpression;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.TreeVisitor;

public class AwaitExpressionImpl extends PyTree implements AwaitExpression {

  private final Token awaitToken;
  private final Expression expression;

  public AwaitExpressionImpl(Token await, Expression expression) {
    this.awaitToken = await;
    this.expression = expression;
  }

  @Override
  public Token awaitToken() {
    return awaitToken;
  }

  @Override
  public Expression expression() {
    return expression;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitAwaitExpression(this);
  }

  @Override
  public List<Tree> computeChildren() {
    return Stream.of(awaitToken, expression).filter(Objects::nonNull).toList();
  }

  @Override
  public Kind getKind() {
    return Kind.AWAIT;
  }
}

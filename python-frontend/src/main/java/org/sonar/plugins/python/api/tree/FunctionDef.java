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
package org.sonar.plugins.python.api.tree;

import java.util.List;
import javax.annotation.CheckForNull;

/**
 * <pre>
 *   {@link #decorators()}
 *   def {@link #name()} ( {@link FunctionLike#parameters()} ) {@link #returnTypeAnnotation()}:
 *     {@link #docstring()}
 *     {@link #body()}
 * </pre>
 *
 * See https://docs.python.org/3/reference/compound_stmts.html#function-definitions
 */
public interface FunctionDef extends Statement, FunctionLike {

  List<Decorator> decorators();

  Token defKeyword();

  @CheckForNull
  Token asyncKeyword();

  Name name();

  @CheckForNull
  TypeParams typeParams();

  Token leftPar();

  Token rightPar();

  /**
   * {@code -> returnType}
   */
  @CheckForNull
  TypeAnnotation returnTypeAnnotation();

  Token colon();

  @CheckForNull
  StringLiteral docstring();

  StatementList body();

}

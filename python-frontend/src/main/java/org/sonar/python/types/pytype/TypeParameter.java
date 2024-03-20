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
package org.sonar.python.types.pytype;

import java.util.List;

public class TypeParameter extends BaseType {
  private List<BaseType> constraints;
  private BaseType bound;
  private String scope;

  public List<BaseType> constraints() {
    return constraints;
  }

  public TypeParameter constraints(List<BaseType> constraints) {
    this.constraints = constraints;
    return this;
  }

  public BaseType bound() {
    return bound;
  }

  public TypeParameter bound(BaseType bound) {
    this.bound = bound;
    return this;
  }

  public String scope() {
    return scope;
  }

  public TypeParameter scope(String scope) {
    this.scope = scope;
    return this;
  }
}

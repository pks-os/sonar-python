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
package org.sonar.python.types.v2;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TriBoolTest {

  @Test
  void andTest() {
    assertThat(TriBool.TRUE.and(TriBool.TRUE)).isEqualTo(TriBool.TRUE);
    assertThat(TriBool.TRUE.and(TriBool.UNKNOWN)).isEqualTo(TriBool.UNKNOWN);
    assertThat(TriBool.TRUE.and(TriBool.FALSE)).isEqualTo(TriBool.FALSE);
    assertThat(TriBool.FALSE.and(TriBool.TRUE)).isEqualTo(TriBool.FALSE);
    assertThat(TriBool.FALSE.and(TriBool.UNKNOWN)).isEqualTo(TriBool.UNKNOWN);
    assertThat(TriBool.FALSE.and(TriBool.FALSE)).isEqualTo(TriBool.FALSE);
    assertThat(TriBool.UNKNOWN.and(TriBool.TRUE)).isEqualTo(TriBool.UNKNOWN);
    assertThat(TriBool.UNKNOWN.and(TriBool.UNKNOWN)).isEqualTo(TriBool.UNKNOWN);
    assertThat(TriBool.UNKNOWN.and(TriBool.FALSE)).isEqualTo(TriBool.UNKNOWN);
  }

  @Test
  void orTest() {
    assertThat(TriBool.TRUE.or(TriBool.TRUE)).isEqualTo(TriBool.TRUE);
    assertThat(TriBool.TRUE.or(TriBool.UNKNOWN)).isEqualTo(TriBool.TRUE);
    assertThat(TriBool.TRUE.or(TriBool.FALSE)).isEqualTo(TriBool.TRUE);
    assertThat(TriBool.FALSE.or(TriBool.TRUE)).isEqualTo(TriBool.TRUE);
    assertThat(TriBool.FALSE.or(TriBool.UNKNOWN)).isEqualTo(TriBool.UNKNOWN);
    assertThat(TriBool.FALSE.or(TriBool.FALSE)).isEqualTo(TriBool.FALSE);
    assertThat(TriBool.UNKNOWN.or(TriBool.TRUE)).isEqualTo(TriBool.TRUE);
    assertThat(TriBool.UNKNOWN.or(TriBool.UNKNOWN)).isEqualTo(TriBool.UNKNOWN);
    assertThat(TriBool.UNKNOWN.or(TriBool.FALSE)).isEqualTo(TriBool.UNKNOWN);
  }
}

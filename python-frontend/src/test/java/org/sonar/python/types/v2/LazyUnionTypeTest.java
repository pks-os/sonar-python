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
package org.sonar.python.types.v2;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.python.semantic.v2.LazyTypesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sonar.python.types.v2.TypesTestUtils.FLOAT_TYPE;
import static org.sonar.python.types.v2.TypesTestUtils.INT_TYPE;

class LazyUnionTypeTest {

  @Test
  void lazyUnionTypeResolvesNestedLazyTypesWhenAccessed() {
    LazyTypesContext lazyTypesContext = Mockito.mock(LazyTypesContext.class);
    when(lazyTypesContext.resolveLazyType(Mockito.any())).thenReturn(INT_TYPE);
    LazyType lazyType = new LazyType("random", lazyTypesContext);
    LazyUnionType lazyUnionType = new LazyUnionType(Set.of(lazyType, FLOAT_TYPE));
    UnionType unionType = (UnionType) lazyUnionType.resolve();
    assertThat(unionType.candidates()).containsExactlyInAnyOrder(INT_TYPE, FLOAT_TYPE);
  }
}

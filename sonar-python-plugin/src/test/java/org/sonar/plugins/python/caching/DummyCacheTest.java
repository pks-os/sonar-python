/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2022 SonarSource SA
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
package org.sonar.plugins.python.caching;


import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DummyCacheTest {

  @Test
  public void read_always_throws_an_IllegalArgumentException() {
    String key = "key";

    DummyCache cache = new DummyCache();
    assertThatThrownBy(() -> cache.read(key))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No cache data available");
  }

  @Test
  public void contains_always_returns_false() {
    DummyCache cache = new DummyCache();
    assertThat(cache.contains("key")).isFalse();
  }

  @Test
  public void write_bytes_throws_an_exception_on_the_second_write_to_the_same_key() {
    String key = "key";
    byte[] data = "data".getBytes(StandardCharsets.UTF_8);
    DummyCache cache = new DummyCache();

    assertThatThrownBy(() -> cache.write(key, data))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Same key cannot be written to multiple times (key)");
  }

  @Test
  public void copyFromPrevious_always_throws_an_IllegalArgumentException() {
    String key = "key";
    DummyCache cache = new DummyCache();

    assertThatThrownBy(() -> cache.copyFromPrevious(key))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No cache data available");
  }

  @Test
  public void readBytes_returns_null() {
    assertThat(new DummyCache().readBytes("foo")).isNull();
  }
}
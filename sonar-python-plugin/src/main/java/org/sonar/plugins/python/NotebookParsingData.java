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
package org.sonar.plugins.python;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.sonar.python.IPythonLocation;

public class NotebookParsingData {

  private StringBuilder aggregatedSource;

  private Map<Integer, IPythonLocation> locationMap;

  private Integer aggregatedSourceLine;

  public NotebookParsingData(StringBuilder aggregatedSource, Map<Integer, IPythonLocation> locationMap, Integer aggregatedSourceLine) {
    this.aggregatedSource = aggregatedSource;
    // Keys are the aggregated source line number
    this.locationMap = locationMap;
    this.aggregatedSourceLine = aggregatedSourceLine;
  }

  public static NotebookParsingData fromLine(int line) {
    return new NotebookParsingData(new StringBuilder(), new LinkedHashMap<>(), line);
  }

  public static NotebookParsingData empty() {
    return new NotebookParsingData(new StringBuilder(), new LinkedHashMap<>(), 0);
  }

  public StringBuilder getAggregatedSource() {
    return aggregatedSource;
  }

  public Map<Integer, IPythonLocation> getLocationMap() {
    return locationMap;
  }

  public Integer getAggregatedSourceLine() {
    return aggregatedSourceLine;
  }

  public NotebookParsingData combine(NotebookParsingData other) {
    aggregatedSource.append(other.aggregatedSource);
    aggregatedSourceLine = other.aggregatedSourceLine;
    locationMap.putAll(other.locationMap);
    return this;
  }

  public void appendToSource(String str) {
    aggregatedSource.append(str);
  }

  public void addLineToSource(String sourceLine, int lineNr, int columnNr, Map<Integer, Integer> colOffset, boolean isCompressed) {
    addLineToSource(sourceLine, new IPythonLocation(lineNr, columnNr, colOffset, isCompressed));
  }

  private void appendLine(String line) {
    aggregatedSource.append(line);
    aggregatedSourceLine++;
  }

  public void addLineToSource(String sourceLine, IPythonLocation location) {
    appendLine(sourceLine);
    locationMap.put(aggregatedSourceLine, location);
  }

  public void addDelimiterToSource(String delimiter, int lineNr, int columnNr) {
    appendLine(delimiter);
    addDefaultLocation(aggregatedSourceLine, lineNr, columnNr);
  }

  public void addDefaultLocation(int line, int lineNr, int columnNr) {
    locationMap.putIfAbsent(line, new IPythonLocation(lineNr, columnNr, Map.of(-1, 0)));
  }

  public void removeTrailingExtraLine() {
    if (!aggregatedSource.isEmpty() && aggregatedSource.charAt(aggregatedSource.length() - 1) == '\n') {
      aggregatedSource.deleteCharAt(aggregatedSource.length() - 1);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotebookParsingData that = (NotebookParsingData) o;
    return aggregatedSource.toString().contentEquals(that.aggregatedSource) &&
      Objects.equals(locationMap, that.locationMap) &&
      Objects.equals(aggregatedSourceLine, that.aggregatedSourceLine);
  }

  @Override
  public int hashCode() {
    return Objects.hash(aggregatedSource, locationMap, aggregatedSourceLine);
  }
}
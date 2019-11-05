/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2019 SonarSource SA
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
package org.sonar.python.checks;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.AssignmentStatement;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.HasSymbol;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.NumericLiteral;
import org.sonar.plugins.python.api.tree.SliceExpression;
import org.sonar.plugins.python.api.tree.SliceItem;
import org.sonar.plugins.python.api.tree.Statement;
import org.sonar.plugins.python.api.tree.StatementList;
import org.sonar.plugins.python.api.tree.StringLiteral;
import org.sonar.plugins.python.api.tree.SubscriptionExpression;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.Tree.Kind;
import org.sonar.python.IssueLocation;
import org.sonar.python.semantic.Symbol;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S4143")
public class OverwrittenCollectionEntryCheck extends PythonSubscriptionCheck {

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Kind.STATEMENT_LIST, ctx -> check(ctx, (StatementList) ctx.syntaxNode()));
  }

  private static void check(SubscriptionContext ctx, StatementList statementList) {
    Map<CollectionKey, List<CollectionWrite>> collectionWrites = new HashMap<>();
    for (Statement statement : statementList.statements()) {
      CollectionWrite write = null;
      if (statement.is(Kind.ASSIGNMENT_STMT)) {
        AssignmentStatement assignment = (AssignmentStatement) statement;
        Expression expression = lhs(assignment);
        write = collectionWrite(assignment, expression);
      }
      if (write != null) {
        collectionWrites.computeIfAbsent(write.collectionKey, k -> new ArrayList<>()).add(write);
      } else {
        reportOverwrites(ctx, collectionWrites);
        collectionWrites.clear();
      }
    }
    reportOverwrites(ctx, collectionWrites);
  }

  private static Expression lhs(AssignmentStatement assignment) {
    return assignment.lhsExpressions().get(0).expressions().get(0);
  }

  @CheckForNull
  private static CollectionWrite collectionWrite(AssignmentStatement assignment, Expression expression) {
    if (expression.is(Kind.SLICE_EXPR)) {
      SliceExpression sliceExpression = (SliceExpression) expression;
      String key = key(sliceExpression.sliceList().slices());
      return collectionWrite(assignment, sliceExpression.object(), key, sliceExpression.leftBracket(), sliceExpression.rightBracket());

    } else if (expression.is(Kind.SUBSCRIPTION)) {
      SubscriptionExpression subscription = (SubscriptionExpression) expression;
      String key = key(subscription.subscripts().expressions());
      return collectionWrite(assignment, subscription.object(), key, subscription.leftBracket(), subscription.rightBracket());

    } else {
      return null;
    }
  }

  private static CollectionWrite collectionWrite(AssignmentStatement assignment, Expression collection, @Nullable String key, Token lBracket, Token rBracket) {
    if (key == null) {
      return null;
    }

    if (collection.is(Kind.SLICE_EXPR, Kind.SUBSCRIPTION)) {
      CollectionWrite nested = collectionWrite(assignment, collection);
      if (nested != null) {
        return new CollectionWrite(nested.collectionKey.nest(key), nested.leftBracket, rBracket, assignment);
      }
    }

    if (collection instanceof HasSymbol) {
      Symbol symbol = ((HasSymbol) collection).symbol();
      if (symbol != null) {
        CollectionKey collectionKey = new CollectionKey(symbol, key);
        return new CollectionWrite(collectionKey, lBracket, rBracket, assignment);
      }
    }

    return null;
  }

  @CheckForNull
  private static String key(List<? extends Tree> subscriptsOrSlices) {
    StringBuilder key = new StringBuilder();
    for (Tree tree : subscriptsOrSlices) {
      String keyElement = key(tree);
      if (keyElement == null) {
        return null;
      }
      key.append(",").append(keyElement);
    }
    return key.toString();
  }

  private static String key(Tree tree) {
    if (tree.is(Kind.NUMERIC_LITERAL)) {
      return ((NumericLiteral) tree).valueAsString();
    } else if (tree.is(Kind.STRING_LITERAL)) {
      return ((StringLiteral) tree).trimmedQuotesValue();
    } else if (tree.is(Kind.NAME)) {
      return ((Name) tree).name();
    } else if (tree.is(Kind.SLICE_ITEM)) {
      SliceItem sliceItem = (SliceItem) tree;
      return Stream.of(sliceItem.lowerBound(), sliceItem.upperBound(), sliceItem.stride())
        .map(e -> e == null ? "" : key(e))
        .collect(Collectors.joining(":"));
    }
    return null;
  }

  private static void reportOverwrites(SubscriptionContext ctx, Map<CollectionKey, List<CollectionWrite>> collectionWrites) {
    collectionWrites.forEach((key, writes) -> {
      if (writes.size() > 1) {
        CollectionWrite firstWrite = writes.get(0);
        CollectionWrite secondWrite = writes.get(1);
        AssignmentStatement assignment = secondWrite.assignment;
        Expression lhs = lhs(assignment);
        if (TreeUtils.hasDescendant(assignment.assignedValue(), t -> CheckUtils.areEquivalent(lhs, t))) {
          return;
        }
        String message = String.format(
          "Verify this is the key that was intended; a value has already been saved for it on line %s.",
          firstWrite.leftBracket.line());
        ctx.addIssue(secondWrite.leftBracket, secondWrite.rightBracket, message)
          .secondary(IssueLocation.preciseLocation(firstWrite.leftBracket, firstWrite.rightBracket, null));
      }
    });
  }

  private static class CollectionKey extends AbstractMap.SimpleImmutableEntry<Symbol, String> {

    private CollectionKey(Symbol collection, String key) {
      super(collection, key);
    }

    private CollectionKey nest(String parentTreeKey) {
      return new CollectionKey(getKey(), getValue() + "/" + parentTreeKey);
    }
  }

  private static class CollectionWrite {
    private final CollectionKey collectionKey;
    private final Token leftBracket;
    private final Token rightBracket;
    private final AssignmentStatement assignment;

    private CollectionWrite(CollectionKey collectionKey, Token leftBracket, Token rightBracket, AssignmentStatement assignment) {
      this.collectionKey = collectionKey;
      this.leftBracket = leftBracket;
      this.rightBracket = rightBracket;
      this.assignment = assignment;
    }
  }
}
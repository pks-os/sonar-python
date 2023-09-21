/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2023 SonarSource SA
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
package org.sonar.python.types;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.plugins.python.api.PythonFile;
import org.sonar.plugins.python.api.cfg.ControlFlowGraph;
import org.sonar.plugins.python.api.symbols.FunctionSymbol;
import org.sonar.plugins.python.api.symbols.Symbol;
import org.sonar.plugins.python.api.symbols.Usage;
import org.sonar.plugins.python.api.tree.AssignmentStatement;
import org.sonar.plugins.python.api.tree.BaseTreeVisitor;
import org.sonar.plugins.python.api.tree.ClassDef;
import org.sonar.plugins.python.api.tree.CompoundAssignmentStatement;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.FileInput;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.Parameter;
import org.sonar.plugins.python.api.tree.QualifiedExpression;
import org.sonar.plugins.python.api.tree.Statement;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.TryStatement;
import org.sonar.plugins.python.api.types.InferredType;
import org.sonar.python.semantic.SymbolImpl;
import org.sonar.python.tree.FunctionDefImpl;
import org.sonar.python.tree.NameImpl;
import org.sonar.python.tree.TreeUtils;

public class TypeInference extends BaseTreeVisitor {

  // The super() builtin is not specified precisely in typeshed.
  // It should return a proxy object (temporary object of the superclass) that allows to access methods of the base class
  // https://docs.python.org/3/library/functions.html#super
  private static final InferredType TYPE_OF_SUPER = InferredTypes.runtimeBuiltinType("super");

  private final Map<Symbol, Set<Assignment>> assignmentsByLhs = new HashMap<>();
  private final Map<QualifiedExpression, MemberAccess> memberAccessesByQualifiedExpr = new HashMap<>();
  private final Map<Statement, Assignment> assignmentsByAssignmentStatement = new HashMap<>();
  private Map<String, InferredType> parameterTypesByName = new HashMap<>();

  public static void inferTypes(FileInput fileInput, PythonFile pythonFile) {
    fileInput.accept(new BaseTreeVisitor() {
      @Override
      public void visitFunctionDef(FunctionDef funcDef) {
        super.visitFunctionDef(funcDef);
        inferTypesAndMemberAccessSymbols(funcDef, pythonFile);
      }
    });
    fileInput.accept(new BaseTreeVisitor() {
      @Override
      public void visitQualifiedExpression(QualifiedExpression qualifiedExpression) {
        super.visitQualifiedExpression(qualifiedExpression);
        Name name = qualifiedExpression.name();
        InferredType type = qualifiedExpression.qualifier().type();
        if (!type.equals(TYPE_OF_SUPER)) {
          Optional<Symbol> resolvedMember = type.resolveMember(name.name());
          resolvedMember.ifPresent(((NameImpl) name)::setSymbol);
        }
      }
    });
  }

  private static Set<Symbol> getTrackedVars(Set<Symbol> localVariables, Set<Name> assignedNames) {
    Set<Symbol> trackedVars = new HashSet<>();
    for (Symbol variable : localVariables) {
      boolean hasMissingBindingUsage = variable.usages().stream()
        .filter(Usage::isBindingUsage)
        .anyMatch(u -> !assignedNames.contains(u.tree()));
      if (!hasMissingBindingUsage) {
        trackedVars.add(variable);
      }
    }
    return trackedVars;
  }

  private static void inferTypesAndMemberAccessSymbols(FunctionDef functionDef, PythonFile pythonFile) {
    TypeInference visitor = new TypeInference();
    functionDef.accept(visitor);
    Set<Name> assignedNames = visitor.assignmentsByLhs.values().stream()
      .flatMap(Collection::stream)
      .map(a -> a.lhsName)
      .collect(Collectors.toSet());

    TryStatementVisitor tryStatementVisitor = new TryStatementVisitor(functionDef);
    functionDef.body().accept(tryStatementVisitor);
    if (tryStatementVisitor.hasTryStatement) {
      // CFG doesn't model precisely try-except statements. Hence we fallback to AST based type inference
      visitor.processPropagations(getTrackedVars(functionDef.localVariables(), assignedNames));
      functionDef.body().accept(new BaseTreeVisitor() {
        @Override
        public void visitFunctionDef(FunctionDef visited) {
          // Don't visit nested functions
        }

        @Override
        public void visitName(Name name) {
          Optional.ofNullable(name.symbol()).ifPresent(symbol ->
            ((NameImpl) name).setInferredType(((SymbolImpl) symbol).inferredType()));
          super.visitName(name);
        }
      });
    } else {
      ControlFlowGraph cfg = ControlFlowGraph.build(functionDef, pythonFile);
      if (cfg == null) {
        return;
      }
      Set<Name> annotatedParamNames = TreeUtils.nonTupleParameters(functionDef).stream()
        .filter(parameter -> parameter.typeAnnotation() != null)
        .map(Parameter::name)
        .collect(Collectors.toSet());
      assignedNames.addAll(annotatedParamNames);
      visitor.flowSensitiveTypeInference(cfg, getTrackedVars(functionDef.localVariables(), assignedNames), functionDef);
    }
  }

  private static class TryStatementVisitor extends BaseTreeVisitor {
    FunctionDef functionDef;
    boolean hasTryStatement = false;

    TryStatementVisitor(FunctionDef functionDef) {
      this.functionDef = functionDef;
    }

    @Override
    public void visitClassDef(ClassDef classDef) {
      // Don't visit nested classes
    }

    @Override
    public void visitFunctionDef(FunctionDef visited) {
      // Don't visit nested functions
    }

    @Override
    public void visitTryStatement(TryStatement tryStatement) {
      hasTryStatement = true;
    }
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    super.visitAssignmentStatement(assignmentStatement);
    if (assignmentStatement.lhsExpressions().stream().anyMatch(expressionList -> !expressionList.commas().isEmpty())) {
      return;
    }
    List<Expression> lhsExpressions = assignmentStatement.lhsExpressions().stream()
      .flatMap(exprList -> exprList.expressions().stream())
      .collect(Collectors.toList());
    if (lhsExpressions.size() != 1) {
      return;
    }
    Expression lhsExpression = lhsExpressions.get(0);
    if (!lhsExpression.is(Tree.Kind.NAME)) {
      return;
    }
    Name lhs = (Name) lhsExpression;
    SymbolImpl symbol = (SymbolImpl) lhs.symbol();
    if (symbol == null) {
      return;
    }

    Expression rhs = assignmentStatement.assignedValue();
    Assignment assignment = new Assignment(symbol, lhs, rhs);
    assignmentsByAssignmentStatement.put(assignmentStatement, assignment);
    assignmentsByLhs.computeIfAbsent(symbol, s -> new HashSet<>()).add(assignment);
  }

  @Override
  public void visitCompoundAssignment(CompoundAssignmentStatement compoundAssignment) {
    super.visitCompoundAssignment(compoundAssignment);
    Expression lhsExpression = compoundAssignment.lhsExpression();
    if (!lhsExpression.is(Tree.Kind.NAME)) {
      return;
    }
    Name lhs = (Name) lhsExpression;
    SymbolImpl symbol = (SymbolImpl) lhs.symbol();
    if (symbol == null) {
      return;
    }

    Expression rhs = compoundAssignment.rhsExpression();
    Assignment assignment = new Assignment(symbol, lhs, rhs);
    assignmentsByAssignmentStatement.put(compoundAssignment, assignment);
    assignmentsByLhs.computeIfAbsent(symbol, s -> new HashSet<>()).add(assignment);
  }

  @Override
  public void visitQualifiedExpression(QualifiedExpression qualifiedExpression) {
    super.visitQualifiedExpression(qualifiedExpression);
    memberAccessesByQualifiedExpr.put(qualifiedExpression, new MemberAccess(qualifiedExpression));
  }

  /**
   * Type inference is computed twice because:
   * - at the end of the first execution target object of member accesses have been resolved to the correct class symbol.
   *
   *   for i in range(3):
   *     if i > 0: b = a.capitalize() # at the end of first execution, type of b is inferred to be "ANY or STR" (hence ANY)
   *     else:     a = 'abc'
   *
   * - at the end of second execution types are inferred again keeping into account resolved method symbols.
   *
   *   for i in range(3):
   *     if i > 0: b = a.capitalize() # at the end of second execution, type of b is inferred to be "STR"
   *     else:     a = 'abc'
   */
  private void flowSensitiveTypeInference(ControlFlowGraph cfg, Set<Symbol> trackedVars, FunctionDef functionDef) {
    Optional.ofNullable(((FunctionDefImpl) functionDef).functionSymbol()).ifPresent(functionSymbol ->
      parameterTypesByName = functionSymbol.parameters()
        .stream()
        .filter(parameter -> parameter.name() != null)
        .collect(Collectors.toMap(FunctionSymbol.Parameter::name, FunctionSymbol.Parameter::declaredType)));

    FlowSensitiveTypeInference flowSensitiveTypeInference =
      new FlowSensitiveTypeInference(trackedVars, memberAccessesByQualifiedExpr, assignmentsByAssignmentStatement, parameterTypesByName);

    flowSensitiveTypeInference.compute(cfg);
    flowSensitiveTypeInference.compute(cfg);
  }

  private void processPropagations(Set<Symbol> trackedVars) {
    Set<Propagation> propagations = new HashSet<>();
    Set<Symbol> initializedVars = new HashSet<>();

    for (MemberAccess memberAccess : memberAccessesByQualifiedExpr.values()) {
      memberAccess.computeDependencies(memberAccess.qualifiedExpression.qualifier(), trackedVars);
      propagations.add(memberAccess);
    }
    assignmentsByLhs.forEach((lhs, as) -> {
      if (trackedVars.contains(lhs)) {
        as.forEach(a -> a.computeDependencies(a.rhs, trackedVars));
        propagations.addAll(as);
      }
    });

    applyPropagations(propagations, initializedVars, true);
    applyPropagations(propagations, initializedVars, false);
  }

  private void applyPropagations(Set<Propagation> propagations, Set<Symbol> initializedVars, boolean checkDependenciesReadiness) {
    Set<Propagation> workSet = new HashSet<>(propagations);
    while (!workSet.isEmpty()) {
      Iterator<Propagation> iterator = workSet.iterator();
      Propagation propagation = iterator.next();
      iterator.remove();
      if (!checkDependenciesReadiness || propagation.areDependenciesReady(initializedVars)) {
        boolean learnt = propagation.propagate(initializedVars);
        if (learnt) {
          workSet.addAll(propagation.dependents());
        }
      }
    }
  }

  private abstract class Propagation {
    private final Set<Symbol> variableDependencies = new HashSet<>();
    private final Set<QualifiedExpression> memberAccessDependencies = new HashSet<>();
    private final Set<Propagation> dependents = new HashSet<>();

    abstract boolean propagate(Set<Symbol> initializedVars);

    void computeDependencies(Expression expression, Set<Symbol> trackedVars) {
      Deque<Expression> workList = new ArrayDeque<>();
      workList.push(expression);
      while (!workList.isEmpty()) {
        Expression e = workList.pop();
        if (e.is(Tree.Kind.NAME)) {
          Name name = (Name) e;
          Symbol symbol = name.symbol();
          if (symbol != null && trackedVars.contains(symbol)) {
            variableDependencies.add(symbol);
            assignmentsByLhs.get(symbol).forEach(a -> a.dependents().add(this));
          }
        } else if (e.is(Tree.Kind.QUALIFIED_EXPR)) {
          QualifiedExpression qualifiedExpression = (QualifiedExpression) e;
          memberAccessDependencies.add(qualifiedExpression);
          memberAccessesByQualifiedExpr.get(qualifiedExpression).dependents().add(this);
        } else if (e instanceof HasTypeDependencies) {
          workList.addAll(((HasTypeDependencies) e).typeDependencies());
        }
      }
    }

    private boolean areDependenciesReady(Set<Symbol> initializedVars) {
      return initializedVars.containsAll(variableDependencies)
        && memberAccessDependencies.stream()
            .map(QualifiedExpression::symbol)
            .allMatch(s -> s != null && s.kind() == Symbol.Kind.FUNCTION);
    }

    Set<Propagation> dependents() {
      return dependents;
    }
  }

  class Assignment extends Propagation {
    final SymbolImpl lhs;
    private final Name lhsName;
    final Expression rhs;

    private Assignment(SymbolImpl lhs, Name lhsName, Expression rhs) {
      this.lhs = lhs;
      this.lhsName = lhsName;
      this.rhs = rhs;
    }

    /** @return true if the propagation effectively changed the inferred type of lhs */
    @Override
    public boolean propagate(Set<Symbol> initializedVars) {
      InferredType rhsType = rhs.type();
      if (initializedVars.add(lhs)) {
        lhs.setInferredType(rhsType);
        return true;
      } else {
        InferredType currentType = lhs.inferredType();
        InferredType newType = InferredTypes.or(rhsType, currentType);
        lhs.setInferredType(newType);
        return !newType.equals(currentType);
      }
    }
  }

  class MemberAccess extends Propagation {

    private final QualifiedExpression qualifiedExpression;
    private final Symbol symbolWithoutTypeInference;

    private MemberAccess(QualifiedExpression qualifiedExpression) {
      this.qualifiedExpression = qualifiedExpression;
      this.symbolWithoutTypeInference = qualifiedExpression.symbol();
    }

    @Override
    public boolean propagate(Set<Symbol> initializedVars) {
      NameImpl name = (NameImpl) qualifiedExpression.name();
      InferredType type = qualifiedExpression.qualifier().type();
      if (!type.equals(TYPE_OF_SUPER)) {
        Optional<Symbol> resolvedMember = type.resolveMember(name.name());
        Symbol previous = name.symbol();
        if (resolvedMember.isPresent()) {
          name.setSymbol(resolvedMember.get());
          return previous != resolvedMember.get();
        } else if (name.symbol() != symbolWithoutTypeInference) {
          name.setSymbol(symbolWithoutTypeInference);
          return true;
        }
      }
      return false;
    }
  }
}

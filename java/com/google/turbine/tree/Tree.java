/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.turbine.tree;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.turbine.model.Const;
import com.google.turbine.model.TurbineConstantTypeKind;
import com.google.turbine.model.TurbineTyKind;
import java.util.Set;

/** An AST node. */
public abstract class Tree {

  public abstract Kind kind();

  public abstract <I, O> O accept(Visitor<I, O> visitor, I input);

  @Override
  public String toString() {
    return Pretty.pretty(this);
  }

  public enum Kind {
    WILD_TY,
    ARR_TY,
    PRIM_TY,
    VOID_TY,
    CLASS_TY,
    LITERAL,
    TYPE_CAST,
    UNARY,
    BINARY,
    CONST_VAR_NAME,
    CLASS_LITERAL,
    ASSIGN,
    CONDITIONAL,
    ARRAY_INIT,
    COMP_UNIT,
    IMPORT_DECL,
    VAR_DECL,
    METH_DECL,
    ANNO,
    ANNO_EXPR,
    TY_DECL,
    TY_PARAM,
    PKG_DECL
  }

  /** A type use. */
  public abstract static class Type extends Tree {}

  /** An expression. */
  public abstract static class Expression extends Tree {}

  /** A wildcard type, possibly with an upper or lower bound. */
  public static class WildTy extends Type {
    private final Optional<Type> upper;
    private final Optional<Type> lower;

    public WildTy(Optional<Type> upper, Optional<Type> lower) {
      this.upper = upper;
      this.lower = lower;
    }

    @Override
    public Kind kind() {
      return Kind.WILD_TY;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitWildTy(this, input);
    }

    /**
     * An optional upper (extends) bound.
     *
     * <p>At most one of {@link #upper} and {@link #lower} will be set.
     */
    public Optional<Type> upper() {
      return upper;
    }

    /**
     * An optional lower (super) bound.
     *
     * <p>At most one of {@link #upper} and {@link #lower} will be set.
     */
    public Optional<Type> lower() {
      return lower;
    }
  }

  /** An array type. */
  public static class ArrTy extends Type {
    private final Type elem;
    private final int dim;

    public ArrTy(Type elem, int dim) {
      this.elem = elem;
      this.dim = dim;
    }

    @Override
    public Kind kind() {
      return Kind.ARR_TY;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitArrTy(this, input);
    }

    /**
     * The element type of the array.
     *
     * <p>This will never be another array; multi-dimensional arrays are represented as a single
     * {@link ArrTy} with {@link #dim} > 1.
     */
    public Type elem() {
      return elem;
    }

    /** The array dimension. */
    public int dim() {
      return dim;
    }
  }

  /** A primitive type. */
  public static class PrimTy extends Type {
    private final TurbineConstantTypeKind tykind;

    public PrimTy(TurbineConstantTypeKind tykind) {
      this.tykind = tykind;
    }

    @Override
    public Kind kind() {
      return Kind.PRIM_TY;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitPrimTy(this, input);
    }

    /** The primtiive type. */
    public TurbineConstantTypeKind tykind() {
      return tykind;
    }
  }

  /** The void type, used only for void-returning methods. */
  public static class VoidTy extends Type {

    public static final VoidTy INSTANCE = new VoidTy();

    @Override
    public Kind kind() {
      return Kind.VOID_TY;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitVoidTy(this, input);
    }

    private VoidTy() {}
  }

  /** A class, enum, interface, or annotation {@link Type}. */
  public static class ClassTy extends Type {
    private final Optional<ClassTy> base;
    private final String name;
    private final ImmutableList<Type> tyargs;

    public ClassTy(Optional<ClassTy> base, String name, ImmutableList<Type> tyargs) {
      this.base = base;
      this.name = name;
      this.tyargs = tyargs;
    }

    @Override
    public Kind kind() {
      return Kind.CLASS_TY;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitClassTy(this, input);
    }

    /**
     * The base type, for qualified type uses.
     *
     * <p>For example, {@code Map.Entry}.
     */
    public Optional<ClassTy> base() {
      return base;
    }

    /** The simple name of the type. */
    public String name() {
      return name;
    }

    /** A possibly empty list of type arguments. */
    public ImmutableList<Type> tyargs() {
      return tyargs;
    }
  }

  /** A JLS 3.10 literal expression. */
  public static class Literal extends Expression {
    private final TurbineConstantTypeKind tykind;
    private final Const value;

    public Literal(TurbineConstantTypeKind tykind, Const value) {
      this.tykind = tykind;
      this.value = value;
    }

    @Override
    public Kind kind() {
      return Kind.LITERAL;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitLiteral(this, input);
    }

    public TurbineConstantTypeKind tykind() {
      return tykind;
    }

    public Const value() {
      return value;
    }
  }

  /** A JLS 15.16 cast expression. */
  public static class TypeCast extends Expression {
    private final Type ty;
    private final Expression expr;

    public TypeCast(Type ty, Expression expr) {
      this.ty = ty;
      this.expr = expr;
    }

    @Override
    public Kind kind() {
      return Kind.TYPE_CAST;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitTypeCast(this, input);
    }

    public Type ty() {
      return ty;
    }

    public Expression expr() {
      return expr;
    }
  }

  /** A JLS 15.14 - 14.15 unary expression. */
  public static class Unary extends Expression {
    private final Expression expr;
    private final TurbineOperatorKind op;

    public Unary(Expression expr, TurbineOperatorKind op) {
      this.expr = expr;
      this.op = op;
    }

    @Override
    public Kind kind() {
      return Kind.UNARY;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitUnary(this, input);
    }

    public Expression expr() {
      return expr;
    }

    public TurbineOperatorKind op() {
      return op;
    }
  }

  /** A JLS 15.17 - 14.24 binary expression. */
  public static class Binary extends Expression {
    private final Expression lhs;
    private final Expression rhs;
    private final TurbineOperatorKind op;

    public Binary(Expression lhs, Expression rhs, TurbineOperatorKind op) {
      this.lhs = lhs;
      this.rhs = rhs;
      this.op = op;
    }

    @Override
    public Kind kind() {
      return Kind.BINARY;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitBinary(this, input);
    }

    public Expression lhs() {
      return lhs;
    }

    public Expression rhs() {
      return rhs;
    }

    public TurbineOperatorKind op() {
      return op;
    }
  }

  /** A JLS 6.5.6.1 simple name that refers to a JSL 4.12.4 constant variable. */
  public static class ConstVarName extends Expression {
    private final ImmutableList<String> name;

    public ConstVarName(ImmutableList<String> name) {
      this.name = name;
    }

    @Override
    public Kind kind() {
      return Kind.CONST_VAR_NAME;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitConstVarName(this, input);
    }

    public ImmutableList<String> name() {
      return name;
    }
  }

  /** A JLS 15.8.2 class literal. */
  public static class ClassLiteral extends Expression {

    private final Type type;

    public ClassLiteral(Type type) {
      this.type = type;
    }

    @Override
    public Kind kind() {
      return Kind.CLASS_LITERAL;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitClassLiteral(this, input);
    }

    public Type type() {
      return type;
    }
  }

  /** A JLS 15.26 assignment expression. */
  public static class Assign extends Expression {
    private final String name;
    private final Expression expr;

    public Assign(String name, Expression expr) {
      this.name = name;
      this.expr = expr;
    }

    @Override
    public Kind kind() {
      return Kind.ASSIGN;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitAssign(this, input);
    }

    public String name() {
      return name;
    }

    public Expression expr() {
      return expr;
    }
  }

  /** A JLS 15.25 conditional expression. */
  public static class Conditional extends Expression {
    private final Expression cond;
    private final Expression iftrue;
    private final Expression iffalse;

    public Conditional(Expression cond, Expression iftrue, Expression iffalse) {
      this.cond = cond;
      this.iftrue = iftrue;
      this.iffalse = iffalse;
    }

    @Override
    public Kind kind() {
      return Kind.CONDITIONAL;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitConditional(this, input);
    }

    public Expression cond() {
      return cond;
    }

    public Expression iftrue() {
      return iftrue;
    }

    public Expression iffalse() {
      return iffalse;
    }
  }

  /** JLS 10.6 array initializer. */
  public static class ArrayInit extends Expression {
    private final ImmutableList<Expression> exprs;

    public ArrayInit(ImmutableList<Expression> exprs) {
      this.exprs = exprs;
    }

    @Override
    public Kind kind() {
      return Kind.ARRAY_INIT;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitArrayInit(this, input);
    }

    public ImmutableList<Expression> exprs() {
      return exprs;
    }
  }

  /** A JLS 7.3 compilation unit. */
  public static class CompUnit extends Tree {
    private final Optional<PkgDecl> pkg;
    private final ImmutableList<ImportDecl> imports;
    private final ImmutableList<TyDecl> decls;
    private final String file;

    public CompUnit(
        Optional<PkgDecl> pkg,
        ImmutableList<ImportDecl> imports,
        ImmutableList<TyDecl> decls,
        String file) {
      this.pkg = pkg;
      this.imports = imports;
      this.decls = decls;
      this.file = file;
    }

    @Override
    public Kind kind() {
      return Kind.COMP_UNIT;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitCompUnit(this, input);
    }

    public Optional<PkgDecl> pkg() {
      return pkg;
    }

    public ImmutableList<ImportDecl> imports() {
      return imports;
    }

    public ImmutableList<TyDecl> decls() {
      return decls;
    }

    public String file() {
      return file;
    }
  }

  /** A JLS 7.5 import declaration. */
  public static class ImportDecl extends Tree {
    private final ImmutableList<String> type;
    private final boolean stat;

    public ImportDecl(ImmutableList<String> type, boolean stat) {
      this.type = type;
      this.stat = stat;
    }

    @Override
    public Kind kind() {
      return Kind.IMPORT_DECL;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitImportDecl(this, input);
    }

    public ImmutableList<String> type() {
      return type;
    }

    public boolean stat() {
      return stat;
    }
  }

  /** A JLS 8.3 field declaration, JLS 8.4.1 formal method parameter, or JLS 14.4 variable. */
  public static class VarDecl extends Tree {
    private final ImmutableSet<TurbineModifier> mods;
    private final ImmutableList<Anno> annos;
    private final Tree ty;
    private final String name;
    private final Optional<Expression> init;

    public VarDecl(
        Set<TurbineModifier> mods,
        ImmutableList<Anno> annos,
        Tree ty,
        String name,
        Optional<Expression> init) {
      this.mods = ImmutableSet.copyOf(mods);
      this.annos = annos;
      this.ty = ty;
      this.name = name;
      this.init = init;
    }

    @Override
    public Kind kind() {
      return Kind.VAR_DECL;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitVarDecl(this, input);
    }

    public ImmutableSet<TurbineModifier> mods() {
      return mods;
    }

    public ImmutableList<Anno> annos() {
      return annos;
    }

    public Tree ty() {
      return ty;
    }

    public String name() {
      return name;
    }

    public Optional<Expression> init() {
      return init;
    }
  }

  /** A JLS 8.4 method declaration. */
  public static class MethDecl extends Tree {
    private final ImmutableSet<TurbineModifier> mods;
    private final ImmutableList<Anno> annos;
    private final ImmutableList<TyParam> typarams;
    private final Optional<Tree> ret;
    private final String name;
    private final ImmutableList<VarDecl> params;
    private final ImmutableList<ClassTy> exntys;
    private final Optional<Tree> defaultValue;

    public MethDecl(
        Set<TurbineModifier> mods,
        ImmutableList<Anno> annos,
        ImmutableList<TyParam> typarams,
        Optional<Tree> ret,
        String name,
        ImmutableList<VarDecl> params,
        ImmutableList<ClassTy> exntys,
        Optional<Tree> defaultValue) {
      this.mods = ImmutableSet.copyOf(mods);
      this.annos = annos;
      this.typarams = typarams;
      this.ret = ret;
      this.name = name;
      this.params = params;
      this.exntys = exntys;
      this.defaultValue = defaultValue;
    }

    @Override
    public Kind kind() {
      return Kind.METH_DECL;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitMethDecl(this, input);
    }

    public ImmutableSet<TurbineModifier> mods() {
      return mods;
    }

    public ImmutableList<Anno> annos() {
      return annos;
    }

    public ImmutableList<TyParam> typarams() {
      return typarams;
    }

    public Optional<Tree> ret() {
      return ret;
    }

    public String name() {
      return name;
    }

    public ImmutableList<VarDecl> params() {
      return params;
    }

    public ImmutableList<ClassTy> exntys() {
      return exntys;
    }

    public Optional<Tree> defaultValue() {
      return defaultValue;
    }
  }

  /** A JLS 9.7 annotation. */
  public static class Anno extends Tree {
    private final ImmutableList<String> name;
    private final ImmutableList<Expression> args;

    public Anno(ImmutableList<String> name, ImmutableList<Expression> args) {
      this.name = name;
      this.args = args;
    }

    @Override
    public Kind kind() {
      return Kind.ANNO;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitAnno(this, input);
    }

    public ImmutableList<String> name() {
      return name;
    }

    public ImmutableList<Expression> args() {
      return args;
    }
  }

  /**
   * An annotation in an expression context, e.g. an annotation literal nested inside another
   * annotation.
   */
  public static class AnnoExpr extends Expression {

    private final Anno value;

    public AnnoExpr(Anno value) {
      this.value = value;
    }

    /** The annotation. */
    public Anno value() {
      return value;
    }

    @Override
    public Kind kind() {
      return Kind.ANNO_EXPR;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitAnno(value, input);
    }
  }

  /** A JLS 7.6 or 8.5 type declaration. */
  public static class TyDecl extends Tree {
    private final ImmutableSet<TurbineModifier> mods;
    private final ImmutableList<Anno> annos;
    private final String name;
    private final ImmutableList<TyParam> typarams;
    private final Optional<ClassTy> xtnds;
    private final ImmutableList<ClassTy> impls;
    private final ImmutableList<Tree> members;
    private final TurbineTyKind tykind;

    public TyDecl(
        Set<TurbineModifier> mods,
        ImmutableList<Anno> annos,
        String name,
        ImmutableList<TyParam> typarams,
        Optional<ClassTy> xtnds,
        ImmutableList<ClassTy> impls,
        ImmutableList<Tree> members,
        TurbineTyKind tykind) {
      this.mods = ImmutableSet.copyOf(mods);
      this.annos = annos;
      this.name = name;
      this.typarams = typarams;
      this.xtnds = xtnds;
      this.impls = impls;
      this.members = members;
      this.tykind = tykind;
    }

    @Override
    public Kind kind() {
      return Kind.TY_DECL;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitTyDecl(this, input);
    }

    public ImmutableSet<TurbineModifier> mods() {
      return mods;
    }

    public ImmutableList<Anno> annos() {
      return annos;
    }

    public String name() {
      return name;
    }

    public ImmutableList<TyParam> typarams() {
      return typarams;
    }

    public Optional<ClassTy> xtnds() {
      return xtnds;
    }

    public ImmutableList<ClassTy> impls() {
      return impls;
    }

    public ImmutableList<Tree> members() {
      return members;
    }

    public TurbineTyKind tykind() {
      return tykind;
    }
  }

  /** A JLS 4.4. type variable declaration. */
  public static class TyParam extends Tree {
    private final String name;
    private final ImmutableList<Tree> bounds;

    public TyParam(String name, ImmutableList<Tree> bounds) {
      this.name = name;
      this.bounds = bounds;
    }

    @Override
    public Kind kind() {
      return Kind.TY_PARAM;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitTyParam(this, input);
    }

    public String name() {
      return name;
    }

    public ImmutableList<Tree> bounds() {
      return bounds;
    }
  }

  /** A JLS 7.4 package declaration. */
  public static class PkgDecl extends Tree {
    private final ImmutableList<String> name;

    public PkgDecl(ImmutableList<String> name) {
      this.name = name;
    }

    @Override
    public Kind kind() {
      return Kind.PKG_DECL;
    }

    @Override
    public <I, O> O accept(Visitor<I, O> visitor, I input) {
      return visitor.visitPkgDecl(this, input);
    }

    public ImmutableList<String> name() {
      return name;
    }
  }

  /** A visitor for {@link Tree}s. */
  public interface Visitor<I, O> {
    O visitWildTy(WildTy visitor, I input);

    O visitArrTy(ArrTy arrTy, I input);

    O visitPrimTy(PrimTy primTy, I input);

    O visitVoidTy(VoidTy primTy, I input);

    O visitClassTy(ClassTy visitor, I input);

    O visitLiteral(Literal literal, I input);

    O visitTypeCast(TypeCast typeCast, I input);

    O visitUnary(Unary unary, I input);

    O visitBinary(Binary binary, I input);

    O visitConstVarName(ConstVarName constVarName, I input);

    O visitClassLiteral(ClassLiteral classLiteral, I input);

    O visitAssign(Assign assign, I input);

    O visitConditional(Conditional conditional, I input);

    O visitArrayInit(ArrayInit arrayInit, I input);

    O visitCompUnit(CompUnit compUnit, I input);

    O visitImportDecl(ImportDecl importDecl, I input);

    O visitVarDecl(VarDecl varDecl, I input);

    O visitMethDecl(MethDecl methDecl, I input);

    O visitAnno(Anno anno, I input);

    O visitTyDecl(TyDecl tyDecl, I input);

    O visitTyParam(TyParam tyParam, I input);

    O visitPkgDecl(PkgDecl pkgDecl, I input);
  }
}

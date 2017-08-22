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

package com.google.turbine.bytecode;

import com.google.common.collect.ImmutableList;
import com.google.turbine.model.Const;
import com.google.turbine.model.Const.ShortValue;
import com.google.turbine.model.Const.StringValue;
import com.google.turbine.model.Const.Value;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A constant pool builder, used when writing class files. */
public class ConstantPool {

  /** The next available constant pool entry. */
  short nextEntry = 1;

  private final Map<String, Short> utf8Pool = new HashMap<>();
  private final Map<Short, Short> classInfoPool = new HashMap<>();
  private final Map<Short, Short> stringPool = new HashMap<>();
  private final Map<Integer, Short> integerPool = new HashMap<>();
  private final Map<Double, Short> doublePool = new HashMap<>();
  private final Map<Float, Short> floatPool = new HashMap<>();
  private final Map<Long, Short> longPool = new HashMap<>();

  private final List<Entry> constants = new ArrayList<>();

  /** The ordered list of constant pool entries. */
  public ImmutableList<Entry> constants() {
    return ImmutableList.copyOf(constants);
  }

  /** The number of constant pool entries the given kind takes up. */
  private static short width(Kind kind) {
    switch (kind) {
      case CLASS_INFO:
      case STRING:
      case INTEGER:
      case UTF8:
      case FLOAT:
        return 1;
      case LONG:
      case DOUBLE:
        // "In retrospect, making 8-byte constants take two constant pool entries
        // was a poor choice." -- JVMS 4.4.5
        return 2;
      default:
        throw new AssertionError(kind);
    }
  }

  /** A constant pool entry. */
  static class Entry {
    private final Kind kind;
    private final Value value;

    Entry(Kind kind, Value value) {
      this.kind = kind;
      this.value = value;
    }

    /** The entry kind. */
    public Kind kind() {
      return kind;
    }

    /** The entry's value. */
    public Value value() {
      return value;
    }
  }

  /** Adds a CONSTANT_Class_info entry to the pool. */
  short classInfo(String value) {
    Objects.requireNonNull(value);
    short utf8 = utf8(value);
    if (classInfoPool.containsKey(utf8)) {
      return classInfoPool.get(utf8);
    }
    short index = insert(new Entry(Kind.CLASS_INFO, new ShortValue(utf8)));
    classInfoPool.put(utf8, index);
    return index;
  }

  /** Adds a CONSTANT_Utf8_info entry to the pool. */
  short utf8(String value) {
    Objects.requireNonNull(value);
    if (utf8Pool.containsKey(value)) {
      return utf8Pool.get(value);
    }
    short index = insert(new Entry(Kind.UTF8, new StringValue(value)));
    utf8Pool.put(value, index);
    return index;
  }

  short integer(int value) {
    if (integerPool.containsKey(value)) {
      return integerPool.get(value);
    }
    short index = insert(new Entry(Kind.INTEGER, new Const.IntValue(value)));
    integerPool.put(value, index);
    return index;
  }

  short longInfo(long value) {
    if (longPool.containsKey(value)) {
      return longPool.get(value);
    }
    short index = insert(new Entry(Kind.LONG, new Const.LongValue(value)));
    longPool.put(value, index);
    return index;
  }

  short doubleInfo(double value) {
    if (doublePool.containsKey(value)) {
      return doublePool.get(value);
    }
    short index = insert(new Entry(Kind.DOUBLE, new Const.DoubleValue(value)));
    doublePool.put(value, index);
    return index;
  }

  short floatInfo(float value) {
    if (floatPool.containsKey(value)) {
      return floatPool.get(value);
    }
    short index = insert(new Entry(Kind.FLOAT, new Const.FloatValue(value)));
    floatPool.put(value, index);
    return index;
  }

  short string(String value) {
    Objects.requireNonNull(value);
    short utf8 = utf8(value);
    if (stringPool.containsKey(utf8)) {
      return stringPool.get(utf8);
    }
    short index = insert(new Entry(Kind.STRING, new ShortValue(utf8)));
    stringPool.put(utf8, index);
    return index;
  }

  private short insert(Entry key) {
    short entry = nextEntry;
    constants.add(key);
    nextEntry += width(key.kind());
    return entry;
  }

  /** Constant pool entry kinds. */
  enum Kind {
    CLASS_INFO(7),
    STRING(8),
    INTEGER(3),
    DOUBLE(6),
    FLOAT(4),
    LONG(5),
    UTF8(1);

    private final short tag;

    Kind(int tag) {
      this.tag = (short) tag;
    }

    /** The JVMS Table 4.4-A tag. */
    public short tag() {
      return tag;
    }
  }
}

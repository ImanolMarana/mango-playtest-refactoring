/*
 * Copyright 2014 mango.jfaster.org
 *
 * The Mango Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.jfaster.mango.util.logging;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * String utility class.
 */
public final class StringUtil {

  private StringUtil() {
    // Unused.
  }

  public static final String NEWLINE;

  static {
    String newLine;

    try {
      newLine = new Formatter().format("%n").toString();
    } catch (Exception e) {
      newLine = "\n";
    }

    NEWLINE = newLine;
  }

  private static final String EMPTY_STRING = "";

  /**
   * Splits the specified {@link String} with the specified delimiter.  This operation is a simplified and optimized
   * version of {@link String#split(String)}.
   */
  public static String[] split(String value, char delim) {
  final int end = value.length();
  final List<String> res = new ArrayList<>();

  int start = 0;
  for (int i = 0; i < end; i++) {
    if (value.charAt(i) == delim) {
      addSubstring(res, value, start, i);
      start = i + 1;
    }
  }

  handleLastElement(res, value, start, end);

  return res.toArray(new String[res.size()]);
}

private static void addSubstring(List<String> res, String value, int start, int i) {
  if (start == i) {
    res.add(EMPTY_STRING);
  } else {
    res.add(value.substring(start, i));
  }
}

private static void handleLastElement(List<String> res, String value, int start, int end) {
  if (start == 0) {
    res.add(value);
  } else {
    if (start != end) {
      res.add(value.substring(start, end));
    } else {
      truncateTrailingEmptyElements(res);
    }
  }
}

private static void truncateTrailingEmptyElements(List<String> res) {
  for (int i = res.size() - 1; i >= 0; i--) {
    if (res.get(i).isEmpty()) {
      res.remove(i);
    } else {
      break;
    }
  }
}

//Refactoring end

  /**
   * The shortcut to {@link #simpleClassName(Class) simpleClassName(o.getClass())}.
   */
  public static String simpleClassName(Object o) {
    return simpleClassName(o.getClass());
  }

  /**
   * Generates a simplified name from a {@link Class}.  Similar to {@link Class#getSimpleName()}, but it works fine
   * with anonymous classes.
   */
  public static String simpleClassName(Class<?> clazz) {
    Package pkg = clazz.getPackage();
    if (pkg != null) {
      return clazz.getName().substring(pkg.getName().length() + 1);
    } else {
      return clazz.getName();
    }
  }
}

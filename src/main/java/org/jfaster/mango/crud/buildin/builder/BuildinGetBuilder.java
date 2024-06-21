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

package org.jfaster.mango.crud.buildin.builder;

import org.jfaster.mango.util.Joiner;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ash
 */
public class BuildinGetBuilder extends AbstractBuildinBuilder {

  private final static String SQL_TEMPLATE = "select %s from #table where %s = :1";
  private final static String BATCH_SQL_TEMPLATE = "select %s from #table where %s in (:1)";

  private final String columnId;

  private final List<String> columns;

  private final boolean isBatch;

  public BuildinGetBuilder(String colId, List<String> cols, boolean isBatch) {
    int index = cols.indexOf(colId);
    if (index < 0) {
      throw new IllegalArgumentException("error column id [" + colId + "]");
    }
    columnId = colId;
    columns = new ArrayList<String>(cols);
    this.isBatch = isBatch;
  }

  @Override
  public String buildSql() {
    String s1 = Joiner.on(", ").join(columns);
    return isBatch ?
        String.format(BATCH_SQL_TEMPLATE, s1, columnId) :
        String.format(SQL_TEMPLATE, s1, columnId);
  }

}

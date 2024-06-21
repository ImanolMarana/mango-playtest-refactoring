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
public class BuildinGetAllBuilder extends AbstractBuildinBuilder {

  private final static String SQL = "select %s from #table";

  private final List<String> columns;

  public BuildinGetAllBuilder(List<String> cols) {
    columns = new ArrayList<String>(cols);
  }

  @Override
  public String buildSql() {
    String s1 = Joiner.on(", ").join(columns);
    return String.format(SQL, s1, s1);
  }

}

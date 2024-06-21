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

package org.jfaster.mango.operator.generator;

import org.jfaster.mango.annotation.Sharding;
import org.jfaster.mango.annotation.ShardingBy;
import org.jfaster.mango.annotation.TableShardingBy;
import org.jfaster.mango.binding.BindingParameter;
import org.jfaster.mango.binding.BindingParameterInvoker;
import org.jfaster.mango.binding.ParameterContext;
import org.jfaster.mango.descriptor.ParameterDescriptor;
import org.jfaster.mango.exception.DescriptionException;
import org.jfaster.mango.exception.IncorrectParameterTypeException;
import org.jfaster.mango.sharding.NotUseShardingStrategy;
import org.jfaster.mango.sharding.NotUseTableShardingStrategy;
import org.jfaster.mango.sharding.TableShardingStrategy;
import org.jfaster.mango.util.reflect.Reflection;
import org.jfaster.mango.util.reflect.TypeToken;
import org.jfaster.mango.util.reflect.TypeWrapper;

import javax.annotation.Nullable;
import java.lang.reflect.Type;

/**
 * @author ash
 */
public class TableGeneratorFactory {

  public TableGenerator getTableGenerator(
        @Nullable Sharding shardingAnno,
        @Nullable String table,
        boolean isSqlUseGlobalTable,
        ParameterContext context) {

    TableShardingStrategy strategy = getTableShardingStrategy(shardingAnno);
    TypeToken<?> strategyToken = getStrategyToken(strategy);

    if (isSqlUseGlobalTable && table == null) {
        throw new DescriptionException("if sql use global table '#table'," +
                " @DB.table must be defined");
    }
    if (strategy != null && table == null) {
        throw new DescriptionException("if @Sharding.tableShardingStrategy is defined, " +
                "@DB.table must be defined");
    }

    TableShardingBy tableShardingByAnno = findTableShardingByAnnotation(context);
    ShardingBy shardingByAnno = null;
    if (tableShardingByAnno == null) {
        shardingByAnno = findShardingByAnnotation(context);
    }

    if (strategy != null) {
        return createShardedTableGenerator(table, context, strategyToken, tableShardingByAnno, shardingByAnno);
    } else {
        return new SimpleTableGenerator(table);
    }
}

private TypeToken<?> getStrategyToken(TableShardingStrategy strategy) {
    if (strategy != null) {
        return TypeToken.of(strategy.getClass()).resolveFatherClass(TableShardingStrategy.class);
    }
    return null;
}

@Nullable
private TableShardingBy findTableShardingByAnnotation(ParameterContext context) {
    for (ParameterDescriptor pd : context.getParameterDescriptors()) {
        TableShardingBy annotation = pd.getAnnotation(TableShardingBy.class);
        if (annotation != null) {
            return annotation;
        }
    }
    return null;
}

@Nullable
private ShardingBy findShardingByAnnotation(ParameterContext context) {
    for (ParameterDescriptor pd : context.getParameterDescriptors()) {
        ShardingBy annotation = pd.getAnnotation(ShardingBy.class);
        if (annotation != null) {
            return annotation;
        }
    }
    return null;
}

private TableGenerator createShardedTableGenerator(
        @Nullable String table, 
        ParameterContext context, 
        TypeToken<?> strategyToken, 
        TableShardingBy tableShardingByAnno, 
        ShardingBy shardingByAnno) {

    String parameterName = getParameterName(context, tableShardingByAnno, shardingByAnno);
    String propertyPath = getPropertyPath(tableShardingByAnno, shardingByAnno);

    if (parameterName != null && propertyPath != null) {
        BindingParameter bp = BindingParameter.create(parameterName, propertyPath, null);
        BindingParameterInvoker invoker = context.getBindingParameterInvoker(bp);
        Type targetType = invoker.getTargetType();
        TypeWrapper tw = new TypeWrapper(targetType);

        validateShardingType(targetType, tw, strategyToken);

        return new ShardedTableGenerator(table, invoker, (TableShardingStrategy) strategyToken.getRawType().cast(strategyToken));
    } else {
        throw new DescriptionException("if @Sharding.tableShardingStrategy is defined, " +
                "need one and only one @TableShardingBy on method's parameter but found, " +
                "please note that @ShardingBy = @TableShardingBy + @DatabaseShardingBy");
    }
}

private String getParameterName(ParameterContext context, TableShardingBy tableShardingByAnno, ShardingBy shardingByAnno) {
    if (tableShardingByAnno != null) {
        return context.getParameterNameByPosition(tableShardingByAnno.annotationType().getAnnotation(ParameterDescriptor.class).position());
    } else if (shardingByAnno != null) {
        return context.getParameterNameByPosition(shardingByAnno.annotationType().getAnnotation(ParameterDescriptor.class).position());
    }
    return null;
}

private String getPropertyPath(TableShardingBy tableShardingByAnno, ShardingBy shardingByAnno) {
    if (tableShardingByAnno != null) {
        return tableShardingByAnno.value();
    } else if (shardingByAnno != null) {
        return shardingByAnno.value();
    }
    return null;
}

private void validateShardingType(Type targetType, TypeWrapper tw, TypeToken<?> strategyToken) {
    Class<?> mappedClass = tw.getMappedClass();
    if (mappedClass == null || tw.canIterable()) {
        throw new IncorrectParameterTypeException("the type of parameter Modified @TableShardingBy is error, " +
                "type is " + targetType + ", " +
                "please note that @ShardingBy = @TableShardingBy + @DatabaseShardingBy");
    }
    TypeToken<?> shardToken = TypeToken.of(targetType);
    if (!strategyToken.isAssignableFrom(shardToken.wrap())) {
        throw new ClassCastException("TableShardingStrategy[" + strategyToken.getRawType() + "]'s " +
                "generic type[" + strategyToken.getType() + "] must be assignable from " +
                "the type of parameter Modified @TableShardingBy [" + shardToken.getType() + "], " +
                "please note that @ShardingBy = @TableShardingBy + @DatabaseShardingBy");
    }
}


@Nullable
private TableShardingStrategy getTableShardingStrategy(@Nullable Sharding shardingAnno) {
    if (shardingAnno == null) {
        return null;
    }
    Class<? extends TableShardingStrategy> strategyClass = shardingAnno.tableShardingStrategy();
    if (!strategyClass.equals(NotUseTableShardingStrategy.class)) {
        return Reflection.instantiateClass(strategyClass);
    }
    strategyClass = shardingAnno.shardingStrategy();
    if (!strategyClass.equals(NotUseShardingStrategy.class)) {
        return Reflection.instantiateClass(strategyClass);
    }
    return null;
}

//Refactoring end

  @Nullable
  private TableShardingStrategy getTableShardingStrategy(@Nullable Sharding shardingAnno) {
    if (shardingAnno == null) {
      return null;
    }
    Class<? extends TableShardingStrategy> strategyClass = shardingAnno.tableShardingStrategy();
    if (!strategyClass.equals(NotUseTableShardingStrategy.class)) {
      TableShardingStrategy strategy = Reflection.instantiateClass(strategyClass);
      return strategy;
    }
    strategyClass = shardingAnno.shardingStrategy();
    if (!strategyClass.equals(NotUseShardingStrategy.class)) {
      TableShardingStrategy strategy = Reflection.instantiateClass(strategyClass);
      return strategy;
    }
    return null;
  }

}

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

package org.jfaster.mango.jdbc;

import org.jfaster.mango.jdbc.exception.*;

import javax.sql.DataSource;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * @author ash
 */
public class SQLErrorCodeSQLExceptionTranslator extends AbstractFallbackSQLExceptionTranslator {

  private final SQLErrorCodes sqlErrorCodes;

  public SQLErrorCodeSQLExceptionTranslator(DataSource dataSource) {
    setFallbackTranslator(new SQLExceptionSubclassTranslator());
    this.sqlErrorCodes = SQLErrorCodesFactory.getInstance().getErrorCodes(dataSource);
  }

  @Override
  protected DataAccessException doTranslate(String sql, SQLException ex) {
    SQLException sqlEx = ex;
    if (sqlEx instanceof BatchUpdateException && sqlEx.getNextException() != null) {
      sqlEx = extractNestedSQLException((BatchUpdateException) sqlEx);
    }
    
    String errorCode = determineErrorCode(sqlEx);
    if (errorCode != null) {
      return translateErrorCode(sql, sqlEx, errorCode);
    }
    
    return fallbackTranslation(sql, sqlEx);
  }
  
  private SQLException extractNestedSQLException(BatchUpdateException batchUpdateEx) {
    SQLException nestedSqlEx = batchUpdateEx.getNextException();
    if (nestedSqlEx.getErrorCode() > 0 || nestedSqlEx.getSQLState() != null) {
      logger.debug("Using nested SQLException from the BatchUpdateException");
      return nestedSqlEx;
    }
    return batchUpdateEx;
  }
  
  private String determineErrorCode(SQLException sqlEx) {
    if (sqlErrorCodes.isUseSqlStateForTranslation()) {
      return sqlEx.getSQLState();
    }
    
    SQLException current = sqlEx;
    while (current.getErrorCode() == 0 && current.getCause() instanceof SQLException) {
      current = (SQLException) current.getCause();
    }
    return Integer.toString(current.getErrorCode());
  }
  
  private DataAccessException translateErrorCode(String sql, SQLException sqlEx, String errorCode) {
    if (Arrays.binarySearch(this.sqlErrorCodes.getBadSqlGrammarCodes(), errorCode) >= 0) {
      logTranslation(sql, sqlEx);
      return new BadSqlGrammarException(sql, sqlEx);
    } else if (Arrays.binarySearch(this.sqlErrorCodes.getInvalidResultSetAccessCodes(), errorCode) >= 0) {
      logTranslation(sql, sqlEx);
      return new InvalidResultSetAccessException(sql, sqlEx);
    } else if (Arrays.binarySearch(this.sqlErrorCodes.getDuplicateKeyCodes(), errorCode) >= 0) {
      logTranslation(sql, sqlEx);
      return new DuplicateKeyException(buildMessage(sql, sqlEx), sqlEx);
    } else if (Arrays.binarySearch(this.sqlErrorCodes.getDataIntegrityViolationCodes(), errorCode) >= 0) {
      logTranslation(sql, sqlEx);
      return new DataIntegrityViolationException(buildMessage(sql, sqlEx), sqlEx);
    } else if (Arrays.binarySearch(this.sqlErrorCodes.getPermissionDeniedCodes(), errorCode) >= 0) {
      logTranslation(sql, sqlEx);
      return new PermissionDeniedDataAccessException(buildMessage(sql, sqlEx), sqlEx);
    } else if (Arrays.binarySearch(this.sqlErrorCodes.getDataAccessResourceFailureCodes(), errorCode) >= 0) {
      logTranslation(sql, sqlEx);
      return new DataAccessResourceFailureException(buildMessage(sql, sqlEx), sqlEx);
    } else if (Arrays.binarySearch(this.sqlErrorCodes.getTransientDataAccessResourceCodes(), errorCode) >= 0) {
      logTranslation(sql, sqlEx);
      return new TransientDataAccessResourceException(buildMessage(sql, sqlEx), sqlEx);
    } else if (Arrays.binarySearch(this.sqlErrorCodes.getCannotAcquireLockCodes(), errorCode) >= 0) {
      logTranslation(sql, sqlEx);
      return new CannotAcquireLockException(buildMessage(sql, sqlEx), sqlEx);
    } else if (Arrays.binarySearch(this.sqlErrorCodes.getDeadlockLoserCodes(), errorCode) >= 0) {
      logTranslation(sql, sqlEx);
      return new DeadlockLoserDataAccessException(buildMessage(sql, sqlEx), sqlEx);
    } else if (Arrays.binarySearch(this.sqlErrorCodes.getCannotSerializeTransactionCodes(), errorCode) >= 0) {
      logTranslation(sql, sqlEx);
      return new CannotSerializeTransactionException(buildMessage(sql, sqlEx), sqlEx);
    }
    return null;
  }
  
  private DataAccessException fallbackTranslation(String sql, SQLException sqlEx) {
    if (logger.isDebugEnabled()) {
      String codes = sqlErrorCodes.isUseSqlStateForTranslation() ?
              "SQL state '" + sqlEx.getSQLState() + "', error code '" + sqlEx.getErrorCode() :
              "Error code '" + sqlEx.getErrorCode() + "'";
      logger.debug("Unable to translate SQLException with " + codes + ", will now try the fallback translator");
    }
    return null;
  }

//Refactoring end

  private void logTranslation(String sql, SQLException sqlEx) {
    if (logger.isDebugEnabled()) {
      logger.debug("Translating SQLException with SQL state '" + sqlEx.getSQLState() +
          "', error code '" + sqlEx.getErrorCode() + "', message [" + sqlEx.getMessage() +
          "]; SQL was [" + sql + "]");
    }
  }

}

package org.apache.flink.playgrounds.ops.function;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.playgrounds.ops.entity.Database;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;

/**
 * @see https://ci.apache.org/projects/flink/flink-docs-stable/dev/stream/operators/asyncio.html
 */
public class AsyncDatabaseRequest extends RichAsyncFunction<String, String> {

  /**
   * The database specific client that can issue concurrent requests with callbacks
   */
  private transient BasicDataSource dataSource;
  private Database database;

  public AsyncDatabaseRequest(Database database) {
    this.database = database;
  }

  @Override
  public void open(Configuration parameters) throws Exception {
    dataSource = new BasicDataSource();
    dataSource.setDriverClassName(database.getDriver());
    dataSource.setUrl(database.getUrl());
    dataSource.setUsername(database.getUsername());
    dataSource.setPassword(database.getPassword());
  }

  @Override
  public void close() throws Exception {
    dataSource.close();
  }

  @Override
  public void asyncInvoke(String key, final ResultFuture<String> resultFuture)
      throws Exception {

    Connection connection = dataSource.getConnection();
    PreparedStatement preparedStatement = connection.prepareStatement(database.getQuery());
    ResultSet resultSet = preparedStatement.executeQuery();
    resultSet.next();
    resultFuture.complete(Collections.singletonList(resultSet.getString(database.getColumn())));
  }
}
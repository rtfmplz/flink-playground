package org.apache.flink.playgrounds.ops.records;

import lombok.Getter;

@Getter
public enum Database {

  MYSQL("com.mysql.cj.jdbc.Driver",
      "jdbc:mysql://mysql:3306/mysql?serverTimezone=UTC&characterEncoding=UTF-8&useSSL=false",
      "root",
      "password",
      "select now()",
      "now()"
  ),
  POSTGRES("org.postgresql.Driver",
      "jdbc:postgresql://postgres:5432/postgres",
      "postgres",
      "password",
      "select now()",
      "now");

  private final String driver;
  private final String url;
  private final String username;
  private final String password;
  private final String query;
  private final String column;


  Database(String driver, String url, String username, String password, String query,
      String column) {
    this.driver = driver;
    this.url = url;
    this.username = username;
    this.password = password;
    this.query = query;
    this.column = column;
  }
}

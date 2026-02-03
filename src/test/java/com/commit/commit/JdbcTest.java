package com.commit.commit;

import java.sql.Connection;
import java.sql.DriverManager;

public class JdbcTest {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");

        Connection conn = DriverManager.getConnection(
            "jdbc:postgresql://db.fuuciatooaawrnkupjms.supabase.co:5432/postgres?sslmode=require",
            "postgres",
            "bhavesh@Commit"
        );

        System.out.println("CONNECTED SUCCESSFULLY");
        conn.close();
    }
}

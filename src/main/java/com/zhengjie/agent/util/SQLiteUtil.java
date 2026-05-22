package com.zhengjie.agent.util;

import org.sqlite.SQLiteConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteUtil {
    // 数据库文件路径
    private static final String DB_PATH = "D:/CODE/javaworkspace/agent-db/qa.db";
    private static final String URL = "jdbc:sqlite:" + DB_PATH;
    // vec0 扩展绝对路径（避免 JVM 工作目录不一致导致找不到 DLL）
    private static final String VEC0_PATH = "D:/CODE/javaworkspace/agent/vec0";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC驱动加载失败", e);
        }
    }

    /**
     * 获取数据库连接，并确保 sqlite-vec 扩展已加载
     */
    public static Connection getConnection() throws SQLException {
        // 用 SQLiteConfig 启用扩展加载（新版 JDBC 驱动必须这样，否则报 not authorized）
        SQLiteConfig config = new SQLiteConfig();
        config.enableLoadExtension(true);
        Connection conn = DriverManager.getConnection(URL, config.toProperties());

        // 加载 vec0 扩展
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT load_extension('" + VEC0_PATH + "')");
            System.out.println("sqlite-vec 扩展加载成功: " + VEC0_PATH);
        } catch (SQLException ex) {
            throw new SQLException(
                "sqlite-vec 扩展加载失败！\n" +
                "  期望路径: " + VEC0_PATH + ".dll\n" +
                "  请确认文件存在，或去 https://github.com/asg017/sqlite-vec/releases 下载。\n" +
                "  原始错误: " + ex.getMessage(), ex);
        }
        return conn;
    }
}

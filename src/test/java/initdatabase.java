import com.zhengjie.agent.util.SQLiteUtil;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.fail;

class initdatabase {

    @Test
    void initDB() {
        try (Connection conn = SQLiteUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE VIRTUAL TABLE IF NOT EXISTS knowledge_vec USING vec0(embedding float[384])");
            stmt.execute("CREATE TABLE IF NOT EXISTS knowledge_meta (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "content TEXT NOT NULL," +
                    "source VARCHAR(255)," +
                    "vec_rowid INTEGER UNIQUE" +
                    ")");
            System.out.println("数据库表初始化成功！");
        } catch (SQLException e) {
            e.printStackTrace();
            fail("数据库建表失败: " + e.getMessage());
        }
    }
}

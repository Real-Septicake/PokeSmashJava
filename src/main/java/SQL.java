import com.mysql.cj.jdbc.MysqlDataSource;
import models.ServerInfoModel;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.util.List;

public final class SQL {
    private static final String URL = "jdbc:mysql://" + System.getenv("DB_HOST") + ":" + System.getenv("DB_PORT") + "/" + System.getenv("DB_NAME");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASS = System.getenv("DB_PASSWORD");
    private static final MysqlDataSource SOURCE;

    public static void main(String[] args) {
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(SOURCE);

        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("guild", "1050929736538402917");
        SqlRowSet set = template.queryForRowSet("SELECT name FROM serverinfo WHERE guildId = :guild;", paramSource);
        List<ServerInfoModel> servers = SQLUtil.rowSetToObjectList(set, ServerInfoModel.class);
        for(ServerInfoModel info : servers) {
            System.out.println(info.getName());
        }
    }

    static {
        SOURCE = new MysqlDataSource();
        SOURCE.setURL(URL);
        SOURCE.setUser(USER);
        SOURCE.setPassword(PASS);
    }
}

import models.Col;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.lang.constant.Constable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SQLUtil {
    public static <T> List<T> resultSetToObjectList(ResultSet set, Class<T> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for(Field field : fields) {
            field.setAccessible(true);
        }

        ArrayList<T> objs = new ArrayList<>();
        try {
            while(set.next()) {
                T obj = clazz.getConstructor().newInstance();

                for(Field field : fields) {
                    Col col = field.getAnnotation(Col.class);
                    if(col != null) {
                        field.set(obj, getField(set, col.name(), col.type()));
                    }
                }

                objs.add(obj);
            }
        } catch(SQLException ignored) {
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return objs;
    }

    public static <T> List<T> rowSetToObjectList(SqlRowSet set, Class<T> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for(Field field : fields) {
            field.setAccessible(true);
        }

        ArrayList<T> objs = new ArrayList<>();
        try {
            while(set.next()) {
                T obj = clazz.getConstructor().newInstance();

                for(Field field : fields) {
                    Col col = field.getAnnotation(Col.class);
                    if(col != null) {
                        field.set(obj, getField(set, col.name(), col.type()));
                    }
                }

                objs.add(obj);
            }
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return objs;
    }

    private static <T extends Constable> T getField(SqlRowSet set, String column, Class<T> type) {
        try {
            if (type.equals(String.class)) {
                return type.cast(Objects.requireNonNullElse(set.getString(column), ""));
            }
            if (type.getSuperclass().equals(Number.class)) {
                if(type.equals(Byte.class)) {
                    return type.cast(set.getByte(column));
                }
                if(type.equals(Short.class)) {
                    return type.cast(set.getShort(column));
                }
                if(type.equals(Integer.class)) {
                    return type.cast(set.getInt(column));
                }
            }
            if(type.equals(Boolean.class)) {
                return type.cast(set.getBoolean(column));
            }
            throw new RuntimeException("Invalid class " + type);
        } catch (InvalidResultSetAccessException e) {
            return null;
        }
    }

    private static <T extends Constable> T getField(ResultSet set, String column, Class<T> type) {
        try {
            if (type.equals(String.class)) {
                return type.cast(Objects.requireNonNullElse(set.getString(column), ""));
            }
            if (type.getSuperclass().equals(Number.class)) {
                if(type.equals(Byte.class)) {
                    return type.cast(set.getByte(column));
                }
                if(type.equals(Short.class)) {
                    return type.cast(set.getShort(column));
                }
                if(type.equals(Integer.class)) {
                    return type.cast(set.getInt(column));
                }
            }
            if(type.equals(Boolean.class)) {
                return type.cast(set.getBoolean(column));
            }
            throw new RuntimeException("Invalid class " + type);
        } catch (SQLException ignored) {
            return null;
        }
    }
}

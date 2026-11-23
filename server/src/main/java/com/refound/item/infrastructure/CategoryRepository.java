package com.refound.item.infrastructure;

import com.refound.item.domain.Category;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Bảng {@code categories}: 14 dòng cố định, chỉ đổi bằng migration, nên đọc một lần rồi giữ trong bộ nhớ. */
@Repository
public class CategoryRepository {

    private final JdbcTemplate jdbc;
    private volatile List<Category> all;
    private volatile Map<String, Category> byCode;

    public CategoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Category> all() {
        load();
        return all;
    }

    public Optional<Category> find(String code) {
        load();
        return Optional.ofNullable(code == null ? null : byCode.get(code));
    }

    private void load() {
        if (all != null) {
            return;
        }
        // hai luồng cùng nạp lần đầu chỉ tốn thêm một truy vấn, kết quả như nhau
        List<Category> rows = jdbc.query(
                "select code, name_vi, group_code, sensitive from refound.categories order by group_code, code",
                (rs, i) -> new Category(rs.getString("code"), rs.getString("name_vi"), rs.getString("group_code"),
                        rs.getBoolean("sensitive")));
        byCode = rows.stream().collect(Collectors.toUnmodifiableMap(Category::code, Function.identity()));
        all = List.copyOf(rows);
    }
}

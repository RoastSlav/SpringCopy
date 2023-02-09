package testing;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DrugoMapper {
    @Select("SELECT * FROM posts")
    public Post[] doSomething();
}

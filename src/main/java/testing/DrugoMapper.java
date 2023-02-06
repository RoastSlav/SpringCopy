package testing;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DrugoMapper {
    @Select("SELECT * FROM posts.posts")
    public default void doSomething() {
        System.out.println("Hello from DrugoMapper");
    }
}

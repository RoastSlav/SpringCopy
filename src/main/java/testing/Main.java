package testing;

import Spring.Anotations.ComponentScan;
import Spring.Anotations.SpringBootApplication;
import Spring.SpringApplication;

@SpringBootApplication
@ComponentScan("testingSecond")
public class Main {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
    }
}

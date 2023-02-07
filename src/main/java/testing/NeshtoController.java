package testing;

import Spring.Anotations.Autowired;
import Spring.Anotations.GetMapping;
import Spring.Anotations.RequestMapping;
import Spring.Anotations.RestController;

@RestController
@RequestMapping("/test")
public class NeshtoController {
    @Autowired
    DrugoMapper mapper;


    @GetMapping("/get")
    public String doSomething() {
        mapper.doSomething();
        return "Hello";
    }
}

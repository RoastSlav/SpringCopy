package testingSecond;

import DepInj.Autowired;
import Spring.Anotations.GetMapping;
import Spring.Anotations.RequestMapping;
import Spring.Anotations.RestController;
import testing.DrugoMapper;

@RestController
@RequestMapping("/btw")
public class BtwController {
    @Autowired
    DrugoMapper mapper;


    @GetMapping("/get")
    public String doSomething() {
        mapper.doSomething();
        return "Hello";
    }
}

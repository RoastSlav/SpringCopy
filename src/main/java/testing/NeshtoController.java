package testing;

import Spring.Anotations.*;

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

    @PostMapping("/post/#{someone}")
    public String doSomethingElse(@PathVariable String someone) {
        System.out.println("Hello " + someone);
        mapper.doSomething();
        return "Hello " + someone;
    }

    @PostMapping("/post")
    public String doSomethingBody(@RequestBody String someone) {
        System.out.println("Hello " + someone);
        mapper.doSomething();
        return "Hello " + someone;
    }
}

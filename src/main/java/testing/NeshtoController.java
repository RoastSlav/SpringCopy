package testing;

import Spring.Anotations.*;

@RestController
@RequestMapping("/test")
public class NeshtoController {
    @Autowired
    DrugoMapper mapper;


    @GetMapping("/get")
    public Post[] doSomething() {
        return mapper.doSomething();
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

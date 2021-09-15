package com.protei.task.systemuser;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "api/user")
public class SystemUserController {
    private final SystemUserService systemUserService;

    @Autowired
    public SystemUserController(SystemUserService systemUserService) {
        this.systemUserService = systemUserService;
    }

    @GetMapping
    public List<SystemUser> getSystemUsers() {
        return systemUserService.getSystemUsers();
    }

    @GetMapping(path = "{id}")
    public SystemUser getSystemUserById(@PathVariable("id") long userId) {
        return systemUserService.getSystemUSerById(userId);
    }

    @PostMapping
    public JsonNode registerNewSystemUser(@RequestBody SystemUser user) {
        return systemUserService.addNewSystemUser(user);
    }

    @DeleteMapping(path = "{id}")
    public void deleteSystemUser(@PathVariable("id") long userId) {
        systemUserService.deleteSystemUser(userId);
    }

    @PutMapping(path = "{id}")
    public void updateSystemUser(
            @PathVariable("id") long userId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber) {
        systemUserService.updateSystemUser(userId, name, email, phoneNumber);
    }

    @PutMapping(path = "status/{id}")
    public JsonNode updateSystemUserStatus(
            @PathVariable("id") long userId,
            @RequestParam() String userStatus
    ) {
        return systemUserService.updateSystemUserStatus(userId, userStatus);
    }
}

package com.mediroute.service.security;

import java.util.List;

public interface AppUserView {
    Long getId();
    String getEmail();
    List<String> getRoles();   // List of simple role names, e.g. ["ADMIN","DISPATCHER"]
    Long getDriverId();        // nullable
    String getName();          // nullable
}
//package com.legacylock.backend.controller;
//
//import com.legacylock.backend.dto.response.SchedulerRunResponse;
//import com.legacylock.backend.service.ReleaseSchedulerService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.security.access.prepost.PreAuthorize;
//
//@RestController
//@RequestMapping("/api/dev/scheduler")
//@RequiredArgsConstructor
//@PreAuthorize("hasRole('ADMIN')")
//public class DevSchedulerController {
//
//    private final ReleaseSchedulerService releaseSchedulerService;
//
//    @PostMapping("/release-check")
//    public ResponseEntity<SchedulerRunResponse> triggerReleaseCheck() {
//        SchedulerRunResponse response = releaseSchedulerService.runReleaseCheck();
//        return ResponseEntity.ok(response);
//    }
//}

// for admin
// package com.legacylock.backend.controller;

// import com.legacylock.backend.dto.response.SchedulerRunResponse;
// import com.legacylock.backend.entity.*;
// import com.legacylock.backend.enums.*;
// import com.legacylock.backend.repository.*;
// import com.legacylock.backend.security.JwtService;
// import com.legacylock.backend.service.EncryptionService;
// import com.legacylock.backend.service.ReleaseSchedulerService;
// import lombok.RequiredArgsConstructor;
// import org.springframework.context.annotation.Profile;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.web.bind.annotation.*;

// import java.time.LocalDateTime;
// import java.util.LinkedHashMap;
// import java.util.Map;
// import java.util.Set;

// @RestController
// @RequestMapping("/api/dev/test-data")
// @RequiredArgsConstructor
// @Profile("dev")
// public class DevTestDataController {

//     private final UserRepository userRepository;
//     private final ReceiverRepository receiverRepository;
//     private final CapsuleRepository capsuleRepository;
//     private final CapsuleReceiverRepository capsuleReceiverRepository;
//     private final ReleasePolicyRepository releasePolicyRepository;
//     private final CheckInRepository checkInRepository;

//     private final PasswordEncoder passwordEncoder;
//     private final JwtService jwtService;
//     private final UserDetailsService userDetailsService;
//     private final EncryptionService encryptionService;
//     private final ReleaseSchedulerService releaseSchedulerService;

//     @PostMapping("/full-flow")
//     public ResponseEntity<Map<String, Object>> createFullFlow() {

//         Users owner = Users.builder()
//                 .name("Kunal Owner")
//                 .email("kunal.owner@test.com")
//                 .password(passwordEncoder.encode("password123"))
//                 .roles(Set.of(Role.OWNER, Role.RECEIVER))
//                 .enabled(true)
//                 .authProvider("LOCAL")
//                 .providerId(null)
//                 .build();

//         owner = userRepository.save(owner);

//         Users receiverUser = Users.builder()
//                 .name("Receiver One")
//                 .email("receiver@test.com")
//                 .password(passwordEncoder.encode("password123"))
//                 .roles(Set.of(Role.OWNER, Role.RECEIVER))
//                 .enabled(true)
//                 .authProvider("LOCAL")
//                 .providerId(null)
//                 .build();

//         receiverUser = userRepository.save(receiverUser);

//         Receiver receiver = Receiver.builder()
//                 .owner(owner)
//                 .name("Receiver One")
//                 .email("receiver@test.com")
//                 .phone("9876543210")
//                 .status(ReceiverStatus.ACTIVE)
//                 .createdAt(LocalDateTime.now())
//                 .updatedAt(LocalDateTime.now())
//                 .build();

//         receiver = receiverRepository.save(receiver);

//         String plainContent = "This is my secret LegacyLock message. Receiver should see this only after release.";

//         Capsule capsule = Capsule.builder()
//                 .owner(owner)
//                 .title("Encrypted File Capsule")
//                 .description("Testing encrypted capsule with dev seed data")
//                 .content(null)
//                 .encryptedContent(encryptionService.encrypt(plainContent))
//                 .encryptionAlgorithm(encryptionService.getAlgorithm())
//                 .contentHash(encryptionService.hashContent(plainContent))
//                 .status(CapsuleStatus.ACTIVE)
//                 .build();

//         capsule = capsuleRepository.save(capsule);

//         CapsuleReceiver capsuleReceiver = CapsuleReceiver.builder()
//                 .capsule(capsule)
//                 .receiver(receiver)
//                 .assignedAt(LocalDateTime.now())
//                 .build();

//         capsuleReceiverRepository.save(capsuleReceiver);

//         ReleasePolicy releasePolicy = ReleasePolicy.builder()
//                 .capsule(capsule)
//                 .inactivityDays(0)
//                 .graceDays(0)
//                 .status(ReleasePolicyStatus.ACTIVE)
//                 .build();

//         releasePolicy = releasePolicyRepository.save(releasePolicy);

//         CheckIn checkIn = CheckIn.builder()
//                 .owner(owner)
//                 .checkedInAt(LocalDateTime.now())
//                 .build();

//         checkInRepository.save(checkIn);

//         SchedulerRunResponse schedulerResult = releaseSchedulerService.runReleaseCheck();

//         UserDetails ownerDetails = userDetailsService.loadUserByUsername(owner.getEmail());
//         UserDetails receiverDetails = userDetailsService.loadUserByUsername(receiverUser.getEmail());

//         String ownerToken = jwtService.generateToken(ownerDetails);
//         String receiverToken = jwtService.generateToken(receiverDetails);

//         Map<String, Object> response = new LinkedHashMap<>();

//         response.put("message", "Dev full flow created successfully");
//         response.put("ownerEmail", owner.getEmail());
//         response.put("receiverEmail", receiverUser.getEmail());
//         response.put("password", "password123");

//         response.put("ownerId", owner.getId());
//         response.put("receiverUserId", receiverUser.getId());
//         response.put("receiverContactId", receiver.getId());
//         response.put("capsuleId", capsule.getId());
//         response.put("releasePolicyId", releasePolicy.getId());

//         response.put("ownerAccessToken", ownerToken);
//         response.put("receiverAccessToken", receiverToken);

//         response.put("schedulerResult", schedulerResult);

//         return ResponseEntity.ok(response);
//     }
// }

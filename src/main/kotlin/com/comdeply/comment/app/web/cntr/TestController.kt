package com.comdeply.comment.app.web.cntr

import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/test")
@Profile("!prod") // 프로덕션 환경에서는 비활성화
class TestController {

    @GetMapping("/hello")
    fun hello(): ResponseEntity<String> {
        return ResponseEntity.ok("Hello World!")
    }

    @PostMapping("/test")
    fun test(@RequestBody body: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf("received" to body))
    }
}

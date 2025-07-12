package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.web.svc.SkinService
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/skins")
@CrossOrigin(originPatterns = ["*"])
class SkinController(
    private val skinService: SkinService
) {

    @GetMapping
    fun getAllSkins(): ResponseEntity<List<SkinDto.SkinResponse>> {
        val skins = skinService.getAllSkins()
        return ResponseEntity.ok(skins)
    }

    @GetMapping("/my")
    fun getMyAccessibleSkins(
        @AuthenticationPrincipal userPrincipal: UserPrincipal?
    ): ResponseEntity<List<SkinDto.SkinResponse>> {
        val adminId = userPrincipal?.id ?: 1L
        val skins = skinService.getSkinsForAdmin(adminId)
        return ResponseEntity.ok(skins)
    }

    @GetMapping("/private")
    fun getMyPrivateSkins(
        @AuthenticationPrincipal userPrincipal: UserPrincipal?
    ): ResponseEntity<List<SkinDto.SkinResponse>> {
        val adminId = userPrincipal?.id ?: 1L
        val skins = skinService.getPrivateSkinsForAdmin(adminId)
        return ResponseEntity.ok(skins)
    }

    @GetMapping("/{id}")
    fun getSkinById(@PathVariable id: Long): ResponseEntity<SkinDto.SkinResponse> {
        return try {
            val skin = skinService.getSkinById(id)
            ResponseEntity.ok(skin)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/name/{name}")
    fun getSkinByName(@PathVariable name: String): ResponseEntity<SkinDto.SkinResponse> {
        return try {
            val skin = skinService.getSkinByName(name)
            ResponseEntity.ok(skin)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createSkin(
        @RequestBody request: SkinDto.CreateSkinRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal?
    ): ResponseEntity<Any> {
        return try {
            val adminId = userPrincipal?.id ?: 1L // 임시로 기본 admin ID 사용
            val createdSkin = skinService.createSkin(request, adminId)
            ResponseEntity.status(HttpStatus.CREATED).body(createdSkin)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "스킨 생성 중 오류가 발생했습니다: ${e.message}"))
        }
    }

    @PutMapping("/{id}")
    fun updateSkin(
        @PathVariable id: Long,
        @RequestBody request: SkinDto.UpdateSkinRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal?
    ): ResponseEntity<Any> {
        return try {
            val adminId = userPrincipal?.id ?: 1L
            val updatedSkin = skinService.updateSkin(id, request, adminId)
            ResponseEntity.ok(updatedSkin)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "스킨 수정 중 오류가 발생했습니다."))
        }
    }

    @PostMapping("/{id}/toggle-sharing")
    fun toggleSkinSharing(
        @PathVariable id: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal?
    ): ResponseEntity<Any> {
        return try {
            val adminId = userPrincipal?.id ?: 1L
            val updatedSkin = skinService.toggleSkinSharing(id, adminId)
            ResponseEntity.ok(updatedSkin)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "스킨 공유 상태 변경 중 오류가 발생했습니다."))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteSkin(
        @PathVariable id: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal?
    ): ResponseEntity<Any> {
        return try {
            val adminId = userPrincipal?.id ?: 1L
            skinService.deleteSkin(id, adminId)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "스킨 삭제 중 오류가 발생했습니다."))
        }
    }

    @PostMapping("/apply")
    fun applySkin(@RequestBody request: SkinDto.SkinApplyRequest): ResponseEntity<Any> {
        return try {
            val application = skinService.applySkin(request)
            ResponseEntity.ok(application)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "스킨 적용 중 오류가 발생했습니다."))
        }
    }

    @GetMapping("/applications/{siteId}")
    fun getSkinApplications(@PathVariable siteId: String): ResponseEntity<List<SkinDto.SkinApplyResponse>> {
        val applications = skinService.getSkinApplications(siteId)
        return ResponseEntity.ok(applications)
    }

    @PostMapping("/sync")
    fun syncSkinsFromFileSystem(): ResponseEntity<Any> {
        return try {
            val result = skinService.scanAndSyncExistingSkinsFromFileSystem()
            ResponseEntity.ok(
                mapOf(
                    "message" to "스킨 동기화가 완료되었습니다.",
                    "details" to result
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "스킨 동기화 중 오류가 발생했습니다: ${e.message}"))
        }
    }
}
